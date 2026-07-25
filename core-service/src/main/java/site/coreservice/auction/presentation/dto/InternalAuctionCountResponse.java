package site.coreservice.auction.presentation.dto;

import site.coreservice.auction.application.dto.InternalAuctionCountResult;

import java.util.List;

public record InternalAuctionCountResponse(
        List<Count> counts
) {
    public static InternalAuctionCountResponse from(List<InternalAuctionCountResult> results) {
        return new InternalAuctionCountResponse(
                results.stream().map(Count::from).toList()
        );
    }

    public record Count(Long productId, long openAuctionCount) {
        static Count from(InternalAuctionCountResult result) {
            return new Count(result.productId(), result.openAuctionCount());
        }
    }
}
