package site.coreservice.auction.application.port.dto;

import site.coreservice.auction.domain.AuctionStatus;

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
        BigDecimal highestBidAmount,
        long bidCount,
        LocalDateTime startAt,
        LocalDateTime endAt
) {
}
