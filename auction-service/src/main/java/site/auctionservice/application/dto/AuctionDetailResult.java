package site.auctionservice.application.dto;

import site.auctionservice.application.port.dto.ProductDetail;
import site.auctionservice.domain.Auction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AuctionDetailResult(
        Long auctionId,
        String itemCondition,
        String itemDescription,
        List<String> itemImages,
        BigDecimal startPrice,
        BigDecimal shippingPrice,
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
                auction.getPricing().getStartPrice().getValue(),
                auction.getPricing().getShippingFee().getValue(),
                auction.getPricing().startBidAmount().getValue(),
                auction.getPricing().getBidUnit().getValue(),
                auction.getStartAt(),
                auction.getEndAt(),
                auction.getSchedule().isExtensionEnabled(),
                auction.getSchedule().getExtensionTime(),
                product,
                auction.getSellerId(),
                sellerNickname,
                detail
        );
    }
}
