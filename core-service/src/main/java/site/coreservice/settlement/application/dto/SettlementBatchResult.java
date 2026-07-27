package site.coreservice.settlement.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import site.coreservice.settlement.domain.SettlementBatch;

public record SettlementBatchResult(Long settlementBatchId, BigDecimal totalAmount,
                                     LocalDateTime periodFrom, LocalDateTime periodTo, LocalDateTime confirmedAt) {

    public static SettlementBatchResult from(SettlementBatch batch) {
        return new SettlementBatchResult(
                batch.getId(),
                batch.getTotalAmount().getValue(),
                batch.getPeriodFrom(),
                batch.getPeriodTo(),
                batch.getConfirmedAt()
        );
    }
}
