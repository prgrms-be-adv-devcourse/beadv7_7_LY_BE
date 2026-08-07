package site.auctionservice.application.dto;

import site.auctionservice.application.port.dto.AuctionProductSummary;
import site.auctionservice.domain.Auction;
import site.auctionservice.domain.AuctionStatus;
import site.auctionservice.domain.Money;

import java.math.BigDecimal;

public record HostedAuctionResult(
        Long auctionId,
        Long productId,
        String title,
        String artistName,
        AuctionStatus status,
        BigDecimal highestBidAmount,
        long bidCount
) {
    public static HostedAuctionResult of(Auction auction, AuctionProductSummary summary, Money highestBidAmount, long bidCount, AuctionStatus status) {
        return new HostedAuctionResult(
                auction.getId(),
                auction.getProductId(),
                summary.title(),
                summary.artistName(),
                status,
                highestBidAmount == null ? null : highestBidAmount.getValue(),
                bidCount
        );
    }
}
