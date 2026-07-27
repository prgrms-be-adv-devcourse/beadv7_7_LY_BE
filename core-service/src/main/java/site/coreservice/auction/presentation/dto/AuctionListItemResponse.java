package site.coreservice.auction.presentation.dto;

import site.coreservice.auction.application.dto.AuctionListItemResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AuctionListItemResponse(
        Long auctionId,
        Long productId,
        String title,
        String artistName,
        Integer releaseYear,
        String genre,
        String pressType,
        String thumbnail,
        Long sellerId,
        String sellerNickname,
        String status,
        BigDecimal finalPrice,
        long bidCount,
        LocalDateTime startAt,
        LocalDateTime endAt
) {
    public static AuctionListItemResponse from(AuctionListItemResult result) {
        return new AuctionListItemResponse(
                result.auctionId(),
                result.productId(),
                result.title(),
                result.artistName(),
                result.releaseYear(),
                result.genre(),
                result.pressType(),
                result.thumbnail(),
                result.sellerId(),
                result.sellerNickname(),
                result.status().name(),
                result.finalPrice().getValue(),
                result.bidCount(),
                result.startAt(),
                result.endAt()
        );
    }
}
