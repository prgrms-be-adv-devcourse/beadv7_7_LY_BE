package site.coreservice.settlement.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.coreservice.settlement.domain.SettlementBatch;
import site.coreservice.settlement.domain.SettlementBatchRepository;

@Repository
@RequiredArgsConstructor
public class SettlementBatchRepositoryImpl implements SettlementBatchRepository {

    private final SettlementBatchJpaRepository settlementBatchJpaRepository;

    @Override
    public SettlementBatch save(SettlementBatch settlementBatch) {
        return settlementBatchJpaRepository.save(settlementBatch);
    }

    @Override
    public Optional<SettlementBatch> findById(Long id) {
        return settlementBatchJpaRepository.findById(id);
    }

    @Override
    public boolean existsBySellerIdAndPeriodFromAndPeriodTo(Long sellerId, LocalDateTime periodFrom, LocalDateTime periodTo) {
        return settlementBatchJpaRepository.existsBySellerIdAndPeriodFromAndPeriodTo(sellerId, periodFrom, periodTo);
    }

    @Override
    public List<SettlementBatch> findAllBySellerId(Long sellerId) {
        return settlementBatchJpaRepository.findAllBySellerIdOrderByConfirmedAtDesc(sellerId);
    }
}
