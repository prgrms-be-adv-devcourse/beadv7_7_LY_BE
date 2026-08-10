package site.pointwalletservice.wallet.infrastructure;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.pointwalletservice.wallet.domain.Wallet;

public interface WalletJpaRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUserId(Long userId);

    /**
     * 잔액을 바꾸는 모든 경로(충전/차감/환원)는 이걸로 조회해야 함 — 조회~save 사이 다른 트랜잭션 진입 차단.
     * 홀드 행 락(HoldJpaRepository.findByAuctionIdForUpdate)과 다르게 여기서는 NOWAIT을 쓰지 않는다 -
     * 같은 유저가 서로 다른 두 경매에 동시에 입찰하는 것도 정책상 정상이라, 두 hold() 호출이 auction_id
     * 레벨에서는 전혀 경쟁이 아니고 지갑 레벨에서만 만난다. 이건 "누가 이기냐"가 아니라 순서대로 처리하면
     * 둘 다 성공해야 하는 상황이라, NOWAIT으로 즉시 실패시키면 아무 문제 없는 유저가 자기 자신의 동시
     * 요청 때문에 스퓨리어스하게 실패 응답을 받는다. 대신 대기 상한선은 무제한 MySQL 기본값이 아니라
     * setLockWaitTimeout()으로 미리 짧게 줄여둔 값을 따른다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.userId = :userId")
    Optional<Wallet> findByUserIdForUpdate(@Param("userId") Long userId);

    /**
     * 이 커넥션(세션)의 InnoDB 락 대기 상한선을 짧게 줄인다. findByUserIdForUpdate() 직전에 매번 호출한다.
     * <p>
     * Hibernate의 MySQL 다이얼렉트는 jakarta.persistence.lock.timeout 힌트를 0(NOWAIT) 아니면 무시,
     * 두 가지로만 처리해서 "N초만 기다려라" 같은 임의 값을 JPA 힌트로는 표현할 수 없다(구글링 블로그
     * https://taetae99.tistory.com/61 참고 - chanyong1027님이 리뷰에서 짚어준 그 이슈). 그래서 MySQL이
     * 실제로 지원하는 세션 변수를 직접 설정한다 - 힌트 없이 그냥 FOR UPDATE만 걸면 MySQL 기본값인
     * innodb_lock_wait_timeout(50초)까지 그대로 블로킹되는데, 정상 흐름(같은 유저의 동시 입찰)에서
     * 실제로 걸리는 시간은 길어야 수백 ms라 50초는 과함 - 그 시간 동안 지갑서비스 스레드/DB 커넥션과
     * 이를 동기 호출한 auction 서비스 스레드까지 같이 묶여서, 뭔가 하나 꼬이면 스레드풀/커넥션풀 고갈로
     * 번질 수 있다. 세션 변수라 이 커넥션이 반납된 뒤 재사용될 때도 값이 남아있지만, 매번 호출 전에
     * 다시 설정하므로 실질적인 문제는 없다.
     */
    @Modifying
    @Query(value = "SET innodb_lock_wait_timeout = :seconds", nativeQuery = true)
    void setLockWaitTimeout(@Param("seconds") int seconds);
}