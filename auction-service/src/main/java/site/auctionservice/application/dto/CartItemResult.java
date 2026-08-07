package site.auctionservice.application.dto;

import site.auctionservice.domain.AuctionStatus;
import site.auctionservice.domain.CartItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CartItemResult(
    Long id,
    Long auctionId,
    AuctionStatus status,
    BigDecimal currentPrice,
    LocalDateTime startAt,
    LocalDateTime endAt
) {

    public static CartItemResult of(CartItem cartItem, InternalAuctionSnapshotResult auction) {
        return new CartItemResult(
            cartItem.getId(),
            cartItem.getAuctionId(),
            auction.status(),
            auction.currentPrice(),
            auction.startAt(),
            auction.endAt()
        );
    }
}
