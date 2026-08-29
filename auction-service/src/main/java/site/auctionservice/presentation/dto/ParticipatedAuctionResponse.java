package site.auctionservice.presentation.dto;

import site.auctionservice.application.dto.ParticipatedAuctionResult;

import java.math.BigDecimal;

public record ParticipatedAuctionResponse(
        Long auctionId,
        Long productId,
        String title,
        String artistName,
        String thumbnail,
        String status,
        BigDecimal myBidAmount,
        String myOutcome,
        BigDecimal highestBidAmount
) {
    public static ParticipatedAuctionResponse from(ParticipatedAuctionResult result) {
        return new ParticipatedAuctionResponse(
                result.auctionId(),
                result.productId(),
                result.title(),
                result.artistName(),
                result.thumbnail(),
                result.status().name(),
                result.myBidAmount().getValue(),
                result.myOutcome().name(),
                result.highestBidAmount() == null ? null : result.highestBidAmount().getValue()
        );
    }
}
