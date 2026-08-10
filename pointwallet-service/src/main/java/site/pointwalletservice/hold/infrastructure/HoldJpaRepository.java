package site.pointwalletservice.hold.infrastructure;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import site.pointwalletservice.hold.domain.Hold;

public interface HoldJpaRepository extends JpaRepository<Hold, Long> {
    Optional<Hold> findByAuctionId(Long auctionId);

    /**
     * 홀드 교체/해제는 이걸로 조회해야 함 — 조회~해제·재저장 사이 다른 트랜잭션 진입 차단.
     * timeout=0(NOWAIT) 이유는 WalletJpaRepository.findByUserIdForUpdate 주석 참고 —
     * MySQL 다이얼렉트에서는 0 아닌 값이 실제 대기시간으로 반영되지 않는다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0"))
    @Query("select h from Hold h where h.auctionId = :auctionId")
    Optional<Hold> findByAuctionIdForUpdate(@Param("auctionId") Long auctionId);
}