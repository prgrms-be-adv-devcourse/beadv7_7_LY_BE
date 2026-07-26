package site.coreservice.settlement.domain;

import java.util.Optional;

public interface SettlementItemRepository {

    SettlementItem save(SettlementItem settlementItem);

    Optional<SettlementItem> findById(Long id);

    Optional<SettlementItem> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);
}
