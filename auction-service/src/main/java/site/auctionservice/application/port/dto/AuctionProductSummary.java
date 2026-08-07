package site.auctionservice.application.port.dto;

public record AuctionProductSummary(
        Long auctionId,
        String title,
        String artistName
) {
}
