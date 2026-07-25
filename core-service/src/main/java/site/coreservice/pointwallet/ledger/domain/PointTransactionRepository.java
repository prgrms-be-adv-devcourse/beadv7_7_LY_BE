package site.coreservice.pointwallet.ledger.domain;
import java.time.LocalDateTime;

public interface PointTransactionRepository {

    PointTransaction save(PointTransaction pointTransaction);

    /** type/from/to는 전부 nullable — null이면 해당 조건 미적용. 정렬은 occurredAt desc 고정. */
    PointTransactionSearchPage search(Long walletId, PointTransactionType type,
                                      LocalDateTime from, LocalDateTime to, int page, int size);
}