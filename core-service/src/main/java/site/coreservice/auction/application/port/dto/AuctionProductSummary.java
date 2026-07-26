package site.coreservice.auction.application.port.dto;

public record AuctionProductSummary(
        Long auctionId,
        String title,
        String artistName
) {
}
