package site.pointwalletservice.ledger.domain;
import java.time.LocalDateTime;

public interface PointTransactionRepository {
    PointTransaction save(PointTransaction pointTransaction);
    PointTransactionSearchPage search(Long walletId, PointTransactionType type,
                                      LocalDateTime from, LocalDateTime to, int page, int size);
    boolean existsByRelatedIdAndType(Long relatedId, PointTransactionType type);
}