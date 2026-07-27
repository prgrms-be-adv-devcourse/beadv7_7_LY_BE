package site.coreservice.auction.application.dto;

import site.coreservice.auction.domain.AuctionStatus;

import java.util.List;

public record CartItemGroupResult(
    AuctionStatus status,
    List<CartItemResult> items
) {

    public static CartItemGroupResult of(AuctionStatus status, List<CartItemResult> items) {
        return new CartItemGroupResult(status, items);
    }

}