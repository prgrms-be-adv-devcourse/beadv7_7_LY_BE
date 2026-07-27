package site.coreservice.auction.application.dto;

import site.coreservice.auction.application.port.dto.ProductDetail;
import site.coreservice.auction.domain.Auction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AuctionDetailResult(
        Long auctionId,
        String itemCondition,
        String itemDescription,
        List<String> itemImages,
        BigDecimal startBidAmount,
        BigDecimal bidUnit,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean extensionEnabled,
        Integer extensionTime,
        ProductDetail product,
        Long sellerId,
        String sellerNickname,
        AuctionStatusDetail detail
) {
    public static AuctionDetailResult of(Auction auction, ProductDetail product, String sellerNickname, AuctionStatusDetail detail) {
        return new AuctionDetailResult(
                auction.getId(),
                auction.getItemInfo().getCondition().name(),
                auction.getItemInfo().getDescription(),
                auction.getItemInfo().getImageUrls(),
                auction.getPricing().startBidAmount().getValue(),
                auction.getPricing().getBidUnit().getValue(),
                auction.getSchedule().getPeriod().getStartAt(),
                auction.getSchedule().getPeriod().getEndAt(),
                auction.getSchedule().isExtensionEnabled(),
                auction.getSchedule().getExtensionTime(),
                product,
                auction.getSellerId(),
                sellerNickname,
                detail
        );
    }
}
