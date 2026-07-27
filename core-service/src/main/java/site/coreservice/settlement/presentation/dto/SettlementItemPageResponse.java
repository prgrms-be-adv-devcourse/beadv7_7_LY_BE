package site.coreservice.settlement.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import site.coreservice.settlement.application.dto.SettlementItemResult;
import site.coreservice.settlement.application.dto.SettlementItemSearchResult;

public record SettlementItemPageResponse(
        List<Item> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {

    public static SettlementItemPageResponse from(SettlementItemSearchResult result) {
        List<Item> items = result.content().stream()
                .map(Item::from)
                .toList();
        return new SettlementItemPageResponse(
                items,
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages(),
                result.last()
        );
    }

    public record Item(
            Long settlementItemId,
            Long orderId,
            BigDecimal finalBidPrice,
            BigDecimal commissionRate,
            BigDecimal commissionAmount,
            BigDecimal netAmount,
            String status,
            LocalDateTime completedAt,
            LocalDateTime paidAt,
            Long settlementBatchId
    ) {

        public static Item from(SettlementItemResult result) {
            return new Item(
                    result.settlementItemId(),
                    result.orderId(),
                    result.finalBidPrice(),
                    result.commissionRate(),
                    result.commissionAmount(),
                    result.netAmount(),
                    result.status(),
                    result.completedAt(),
                    result.paidAt(),
                    result.settlementBatchId()
            );
        }
    }
}
