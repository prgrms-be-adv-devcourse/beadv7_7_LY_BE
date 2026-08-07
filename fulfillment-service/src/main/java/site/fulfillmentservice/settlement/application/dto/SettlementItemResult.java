package site.fulfillmentservice.settlement.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import site.fulfillmentservice.settlement.domain.SettlementItem;

public record SettlementItemResult(
        Long settlementItemId,
        Long orderId,
        BigDecimal finalBidPrice,
        BigDecimal commissionRate,
        BigDecimal commissionAmount,
        BigDecimal netAmount,
        String status,
        LocalDateTime completedAt,
        LocalDateTime confirmedAt,
        Long settlementBatchId
) {

    public static SettlementItemResult from(SettlementItem item) {
        return new SettlementItemResult(
                item.getId(),
                item.getOrderId(),
                item.getFinalBidPrice().getValue(),
                item.getCommissionRate(),
                item.getCommissionAmount().getValue(),
                item.getNetAmount().getValue(),
                item.getStatus().name(),
                item.getCompletedAt(),
                item.getConfirmedAt(),
                item.getSettlementBatchId()
        );
    }
}
