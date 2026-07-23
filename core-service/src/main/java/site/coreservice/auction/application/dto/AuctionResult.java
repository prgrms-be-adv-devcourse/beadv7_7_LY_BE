package site.coreservice.auction.application.dto;

import site.coreservice.auction.domain.Auction;

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
