package site.coreservice.auction.application.dto;

import site.coreservice.auction.application.port.dto.AuctionListSummary;
import site.coreservice.auction.domain.AuctionStatus;
import site.coreservice.auction.domain.Money;

import java.time.LocalDateTime;

public record AuctionListResult(
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
        Money finalPrice,
        long bidCount,
        LocalDateTime startAt,
        LocalDateTime endAt

) {
    public static AuctionListResult from(AuctionListSummary summary) {
        return new AuctionListResult(
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
                Money.from(summary.highestBidAmount()),
                summary.bidCount(),
                summary.startAt(),
                summary.endAt()
        );
    }
}
