package site.pointwalletservice.wallet.infrastructure;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import site.pointwalletservice.wallet.domain.Wallet;

public interface WalletJpaRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUserId(Long userId);

    /**
     * 잔액을 바꾸는 모든 경로(충전/차감/환원)는 이걸로 조회해야 함 — 조회~save 사이 다른 트랜잭션 진입 차단.
     * timeout=0(NOWAIT)을 쓴다 - Hibernate의 MySQL 다이얼렉트는 jakarta.persistence.lock.timeout
     * 힌트를 0(NOWAIT) 아니면 무시 두 가지로만 처리해서, 임의 밀리초 값(예: 3000)을 줘도 실제 대기시간으로
     * 반영되지 않고 MySQL의 innodb_lock_wait_timeout(기본 50초)까지 그냥 블로킹된다. 즉시 실패시키고
     * 호출자(비관적 락 예외를 받는 쪽)가 재시도/에러 응답으로 처리하는 게 훨씬 예측 가능하다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0"))
    @Query("select w from Wallet w where w.userId = :userId")
    Optional<Wallet> findByUserIdForUpdate(@Param("userId") Long userId);
}