package site.coreservice.auction.application.dto;

import site.coreservice.auction.application.port.dto.AuctionProductSummary;
import site.coreservice.auction.domain.Auction;
import site.coreservice.auction.domain.AuctionStatus;
import site.coreservice.auction.domain.Money;

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
