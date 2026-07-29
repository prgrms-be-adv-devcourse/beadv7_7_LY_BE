package site.coreservice.auction.application.dto;

public record AuctionListQuery(
        Long productId,
        String genre,
        String pressType,
        String status,
        String sort
) {
}
