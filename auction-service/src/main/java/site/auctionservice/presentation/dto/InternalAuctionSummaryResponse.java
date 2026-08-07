package site.auctionservice.presentation.dto;

import site.auctionservice.application.dto.InternalAuctionSummaryResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InternalAuctionSummaryResponse(
        Long auctionId,
        Long productId,
        String itemCondition,
        long bidCount,
        BigDecimal finalPrice,
        LocalDateTime endAt,
        String status
) {
    public static InternalAuctionSummaryResponse from(InternalAuctionSummaryResult result) {
        return new InternalAuctionSummaryResponse(
                result.auctionId(),
                result.productId(),
                result.itemCondition(),
                result.bidCount(),
                result.finalPrice(),
                result.endAt(),
                result.status()
        );
    }
}
