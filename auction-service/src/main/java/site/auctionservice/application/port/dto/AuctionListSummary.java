package site.auctionservice.application.port.dto;

import site.auctionservice.domain.AuctionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AuctionListSummary(
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
        BigDecimal highestBidAmount,
        BigDecimal startPrice,
        long bidCount,
        LocalDateTime startAt,
        LocalDateTime endAt
) {
}
