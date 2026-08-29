package site.auctionservice.application.dto;

import site.auctionservice.application.port.dto.AuctionListSummary;
import site.auctionservice.domain.AuctionStatus;
import site.auctionservice.domain.Money;

import java.time.LocalDateTime;

public record AuctionListItemResult(
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
        AuctionStatus status,
        String itemCondition,
        Money finalPrice,
        Money startPrice,
        long bidCount,
        LocalDateTime startAt,
        LocalDateTime endAt

) {
    public static AuctionListItemResult from(AuctionListSummary summary) {
        return new AuctionListItemResult(
                summary.auctionId(),
                summary.productId(),
                summary.title(),
                summary.artistName(),
                summary.releaseYear(),
                summary.genre(),
                summary.pressType(),
                summary.thumbnail(),
                summary.sellerId(),
                summary.sellerNickname(),
                summary.status(),
                summary.itemCondition(),
                Money.from(summary.highestBidAmount()),
                Money.from(summary.startPrice()),
                summary.bidCount(),
                summary.startAt(),
                summary.endAt()
        );
    }
}
