package site.coreservice.auction.application.dto;

public record AuctionListQuery(
        String genre,
        String pressType,
        String status,
        String sort
) {
}
