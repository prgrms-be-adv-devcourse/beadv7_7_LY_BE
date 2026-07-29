package site.coreservice.product.infrastructure.client.dto;

import java.util.List;

/** 경매 counts API의 data 부분. */
public record AuctionCountsPayload(List<Count> counts) {

    public record Count(Long productId, long openAuctionCount) {
    }
}
