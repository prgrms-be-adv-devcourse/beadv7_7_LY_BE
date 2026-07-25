package site.coreservice.auction.presentation.dto;

import java.util.List;
import site.coreservice.auction.application.dto.CartItemGroupResult;

public record WatchedAuctionGroupResponse(String status, List<WatchedAuctionResponse> items) {

    public static WatchedAuctionGroupResponse from(CartItemGroupResult result) {
        List<WatchedAuctionResponse> responses = result.items().stream()
            .map(WatchedAuctionResponse::from)
            .toList();
        return new WatchedAuctionGroupResponse(result.status().name(), responses);
    }
}
