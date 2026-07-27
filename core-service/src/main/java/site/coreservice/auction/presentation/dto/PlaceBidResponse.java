package site.coreservice.auction.presentation.dto;

import site.coreservice.auction.application.dto.PlaceBidResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PlaceBidResponse(
        Long bidId,
        Long auctionId,
        BigDecimal bidAmount,
        String outcome,
        BigDecimal nextMinBidAmount,
        LocalDateTime placedAt,
        LocalDateTime endAt,
        boolean extended
) {
    public static PlaceBidResponse from(PlaceBidResult result) {
        return new PlaceBidResponse(
                result.bidId(),
                result.auctionId(),
                result.bidAmount().getValue(),
                result.outcome().name(),
                result.nextMinBidAmount().getValue(),
                result.placedAt(),
                result.endAt(),
                result.extended()
        );
    }
}
