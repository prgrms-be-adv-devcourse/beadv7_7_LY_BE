package site.pointwalletservice.wallet.infrastructure;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
     * 요청 때문에 스퓨리어스하게 실패 응답을 받는다. 힌트 없이 기본 블로킹(MySQL의
     * innodb_lock_wait_timeout, 기본 50초)으로 두고 정상적으로는 앞 트랜잭션이 끝나며 곧바로 풀리길
     * 기대한다 - 실제로 몇십 초씩 막히면 그건 버그(트랜잭션이 안 끝나고 있다는 뜻)라 이 예외가 그 안전망
     * 역할을 한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.userId = :userId")
    Optional<Wallet> findByUserIdForUpdate(@Param("userId") Long userId);
}