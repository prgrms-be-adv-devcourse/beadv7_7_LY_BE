package site.coreservice.auction.application.dto;

import site.coreservice.auction.domain.Auction;
import site.coreservice.auction.domain.Bid;
import site.coreservice.auction.domain.BidOutcome;
import site.coreservice.auction.domain.Money;

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
