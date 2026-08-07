package site.auctionservice.application.dto;

import site.auctionservice.domain.Auction;
import site.auctionservice.domain.Bid;
import site.auctionservice.domain.BidOutcome;
import site.auctionservice.domain.Money;

import java.time.LocalDateTime;

public record PlaceBidResult(
        Long bidId,
        Long auctionId,
        Money bidAmount,
        BidOutcome outcome,
        Money nextMinBidAmount,
        LocalDateTime placedAt,
        LocalDateTime endAt,
        boolean extended
) {
    public static PlaceBidResult of(Bid bid, Auction auction, Money bidAmount, LocalDateTime endAtAfter, boolean extended) {
        return new PlaceBidResult(
                bid.getId(),
                auction.getId(),
                bidAmount,
                bid.getOutcome(),
                auction.getPricing().nextMinBidAmount(auction.getHighestBid()),
                bid.getPlacedAt(),
                endAtAfter,
                extended
        );
    }
}
