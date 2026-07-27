package site.coreservice.settlement.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import site.coreservice.settlement.application.dto.SettlementBatchResult;

public record SettlementBatchResponse(
        Long settlementBatchId,
        BigDecimal totalAmount,
        LocalDateTime periodFrom,
        LocalDateTime periodTo,
        LocalDateTime confirmedAt
) {

    public static SettlementBatchResponse from(SettlementBatchResult result) {
        return new SettlementBatchResponse(
                result.settlementBatchId(),
                result.totalAmount(),
                result.periodFrom(),
                result.periodTo(),
                result.confirmedAt()
        );
    }
}
