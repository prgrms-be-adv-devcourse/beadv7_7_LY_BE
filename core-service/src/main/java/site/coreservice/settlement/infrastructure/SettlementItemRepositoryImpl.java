package site.coreservice.settlement.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import site.coreservice.settlement.domain.SettlementItem;
import site.coreservice.settlement.domain.SettlementItemRepository;
import site.coreservice.settlement.domain.SettlementItemSearchPage;
import site.coreservice.settlement.domain.SettlementStatus;

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

    @Override
    public List<SettlementItem> findAllByStatusAndCompletedAtBefore(SettlementStatus status, LocalDateTime completedAt) {
        return settlementItemJpaRepository.findAllByStatusAndCompletedAtBefore(status, completedAt);
    }

    @Override
    public List<SettlementItem> findAllByStatusAndCompletedAtBeforeAndSellerId(SettlementStatus status, LocalDateTime completedAt, Long sellerId) {
        return settlementItemJpaRepository.findAllByStatusAndCompletedAtBeforeAndSellerId(status, completedAt, sellerId);
    }

    @Override
    public SettlementItemSearchPage search(Long sellerId, SettlementStatus status,
                                            LocalDateTime from, LocalDateTime to, int page, int size) {
        Page<SettlementItem> result = settlementItemJpaRepository.searchBySellerId(
                sellerId, status, from, to, PageRequest.of(page, size));
        return new SettlementItemSearchPage(result.getContent(), result.getTotalElements());
    }
}
