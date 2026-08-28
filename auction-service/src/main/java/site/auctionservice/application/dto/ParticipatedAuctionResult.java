package site.auctionservice.application.dto;

import site.auctionservice.domain.*;
import site.auctionservice.application.port.dto.AuctionProductSummary;

import java.util.List;

public record ParticipatedAuctionResult(
        Long auctionId,
        Long productId,
        String title,
        String artistName,
        String thumbnail,
        AuctionStatus status,
        Money myBidAmount,
        BidOutcome myOutcome,
        Money highestBidAmount
) {
    public static ParticipatedAuctionResult of(Auction auction, Bid bid, AuctionProductSummary summary, AuctionStatus status) {
        List<String> imageUrls = auction.getItemInfo().getImageUrls();
        String thumbnail = (imageUrls == null || imageUrls.isEmpty()) ? null : imageUrls.getFirst();
        return new ParticipatedAuctionResult(
                auction.getId(),
                auction.getProductId(),
                summary.title(),
                summary.artistName(),
                thumbnail,
                status,
                bid.getAmount(),
                bid.getOutcome(),
                auction.hasBid() ? auction.getHighestBid().getAmount() : null
        );
    }
}
