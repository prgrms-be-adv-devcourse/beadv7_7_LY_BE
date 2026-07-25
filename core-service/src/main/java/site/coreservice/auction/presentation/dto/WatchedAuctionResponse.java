package site.coreservice.auction.presentation.dto;

import site.coreservice.auction.application.dto.CartItemResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WatchedAuctionResponse(
    Long id,
    Long auctionId,
    String status,
    BigDecimal currentPrice,
    LocalDateTime startAt,
    LocalDateTime endAt
) {

    public static WatchedAuctionResponse from(CartItemResult result) {
        return new WatchedAuctionResponse(
            result.id(),
            result.auctionId(),
            result.status().name(),
            result.currentPrice(),
            result.startAt(),
            result.endAt()
        );
    }
}
