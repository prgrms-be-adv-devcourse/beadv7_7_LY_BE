package site.auctionservice.application.dto;

import site.auctionservice.domain.AuctionStatus;

import java.util.List;

public record CartItemGroupResult(
    AuctionStatus status,
    List<CartItemResult> items
) {

    public static CartItemGroupResult of(AuctionStatus status, List<CartItemResult> items) {
        return new CartItemGroupResult(status, items);
    }

}