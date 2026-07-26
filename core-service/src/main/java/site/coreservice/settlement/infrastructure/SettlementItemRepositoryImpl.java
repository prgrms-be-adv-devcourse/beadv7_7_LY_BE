package site.coreservice.settlement.infrastructure;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.coreservice.settlement.domain.SettlementItem;
import site.coreservice.settlement.domain.SettlementItemRepository;

@Repository
@RequiredArgsConstructor
public class SettlementItemRepositoryImpl implements SettlementItemRepository {

    private final SettlementItemJpaRepository settlementItemJpaRepository;

    @Override
    public SettlementItem save(SettlementItem settlementItem) {
        return settlementItemJpaRepository.save(settlementItem);
    }

    @Override
    public Optional<SettlementItem> findById(Long id) {
        return settlementItemJpaRepository.findById(id);
    }

    @Override
    public Optional<SettlementItem> findByOrderId(Long orderId) {
        return settlementItemJpaRepository.findByOrderId(orderId);
    }

    @Override
    public boolean existsByOrderId(Long orderId) {
        return settlementItemJpaRepository.existsByOrderId(orderId);
    }
}
