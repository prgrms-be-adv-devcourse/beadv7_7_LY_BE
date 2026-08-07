package site.auctionservice.presentation.dto;

import site.auctionservice.application.dto.AuctionResult;

public record AuctionResultResponse(
        Long auctionId,
        String status
) {
    public static AuctionResultResponse from(AuctionResult result) {
        return new AuctionResultResponse(
                result.id(),
                result.status()
        );
    }
}
