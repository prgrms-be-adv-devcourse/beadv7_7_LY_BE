package site.coreservice.settlement.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SettlementItemRepository {

    SettlementItem save(SettlementItem settlementItem);

    Optional<SettlementItem> findById(Long id);

    Optional<SettlementItem> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);

    List<SettlementItem> findAllByStatusAndCompletedAtBefore(SettlementStatus status, LocalDateTime completedAt);
}
