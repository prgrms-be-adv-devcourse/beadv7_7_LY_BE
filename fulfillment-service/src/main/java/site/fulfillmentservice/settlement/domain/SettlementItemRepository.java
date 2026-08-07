package site.fulfillmentservice.settlement.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SettlementItemRepository {

    SettlementItem save(SettlementItem settlementItem);

    Optional<SettlementItem> findById(Long id);

    Optional<SettlementItem> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);

    List<Long> findDistinctSellerIdsByStatusAndCompletedAtBefore(SettlementStatus status, LocalDateTime completedAt);

    List<SettlementItem> findAllByStatusAndCompletedAtBeforeAndSellerId(SettlementStatus status, LocalDateTime completedAt, Long sellerId);

    /** status/from/to는 전부 nullable — null이면 해당 조건 미적용. 정렬은 completedAt desc, id desc 고정. */
    SettlementItemSearchPage search(Long sellerId, SettlementStatus status,
                                     LocalDateTime from, LocalDateTime to, int page, int size);
}
