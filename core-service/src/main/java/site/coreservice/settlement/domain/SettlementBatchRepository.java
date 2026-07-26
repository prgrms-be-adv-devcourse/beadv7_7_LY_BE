package site.coreservice.settlement.domain;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SettlementBatchRepository {

    SettlementBatch save(SettlementBatch settlementBatch);

    Optional<SettlementBatch> findById(Long id);

    boolean existsBySellerIdAndPeriodFromAndPeriodTo(Long sellerId, LocalDateTime periodFrom, LocalDateTime periodTo);
}
