package site.auctionservice.presentation.dto;

import site.auctionservice.application.dto.HostedAuctionResult;

import java.math.BigDecimal;

public record HostedAuctionResponse(
        Long auctionId,
        Long productId,
        String title,
        String artistName,
        String status,
        BigDecimal highestBidAmount,
        long bidCount
) {
    public static HostedAuctionResponse from(HostedAuctionResult result) {
        return new HostedAuctionResponse(
                result.auctionId(),
                result.productId(),
                result.title(),
                result.artistName(),
                result.status().name(),
                result.highestBidAmount(),
                result.bidCount()
        );
    }
}
