package site.pointwalletservice.ledger.domain;
import java.time.LocalDateTime;
import java.util.Optional;

public interface PointTransactionRepository {
    PointTransaction save(PointTransaction pointTransaction);
    PointTransactionSearchPage search(Long walletId, PointTransactionType type,
                                      LocalDateTime from, LocalDateTime to, int page, int size);
    boolean existsByRelatedIdAndType(Long relatedId, PointTransactionType type);

    /** holdId(=related_id)로 HOLD 원장의 auctionId를 조회 — Hold 롤백 시 Hold 행 삭제 여부와 무관하게 원장만으로 재구성. */
    Optional<Long> findAuctionIdByHoldId(Long holdId);
}