package site.auctionservice.application.dto;

import site.auctionservice.domain.Auction;

public record AuctionResult(
        Long id,
        String status

) {
    public static AuctionResult from(Auction auction) {
        return new AuctionResult(
                auction.getId(),
                auction.getStatus().name()
        );
    }
}
