package site.auctionservice.presentation.dto;

import site.auctionservice.application.dto.CartItemGroupResult;
import site.auctionservice.presentation.dto.WatchedAuctionResponse;

import java.util.List;

public record WatchedAuctionGroupResponse(String status, List<WatchedAuctionResponse> items) {

    public static WatchedAuctionGroupResponse from(CartItemGroupResult result) {
        List<WatchedAuctionResponse> responses = result.items().stream()
            .map(WatchedAuctionResponse::from)
            .toList();
        return new WatchedAuctionGroupResponse(result.status().name(), responses);
    }
}
