package site.fulfillmentservice.settlement.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SettlementBatchRepository {

    SettlementBatch save(SettlementBatch settlementBatch);

    Optional<SettlementBatch> findById(Long id);

    boolean existsBySellerIdAndPeriodFromAndPeriodTo(Long sellerId, LocalDateTime periodFrom, LocalDateTime periodTo);

    List<SettlementBatch> findAllBySellerId(Long sellerId);
}
