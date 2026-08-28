package site.auctionservice.application.dto;

public record AuctionListQuery(
        Long sellerId,
        Long productId,
        String genre,
        String pressType,
        String status,
        String sort
) {
}
