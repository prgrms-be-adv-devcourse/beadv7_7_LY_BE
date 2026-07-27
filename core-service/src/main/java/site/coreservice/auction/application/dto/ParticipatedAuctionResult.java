package site.coreservice.auction.application.dto;

import site.coreservice.auction.application.port.dto.AuctionProductSummary;
import site.coreservice.auction.domain.*;

public record ParticipatedAuctionResult(
        Long auctionId,
        Long productId,
        String title,
        String artistName,
        AuctionStatus status,
        Money myBidAmount,
        BidOutcome myOutcome
) {
    public static ParticipatedAuctionResult of(Auction auction, Bid bid, AuctionProductSummary summary, AuctionStatus status) {
        return new ParticipatedAuctionResult(
                auction.getId(),
                auction.getProductId(),
                summary.title(),
                summary.artistName(),
                status,
                bid.getAmount(),
                bid.getOutcome()
        );
    }
}
