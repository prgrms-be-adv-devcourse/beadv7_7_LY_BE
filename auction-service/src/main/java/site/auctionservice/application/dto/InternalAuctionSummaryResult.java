package site.auctionservice.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InternalAuctionSummaryResult(
        Long auctionId,
        Long productId,
        String itemCondition,
        long bidCount,
        BigDecimal finalPrice,
        LocalDateTime endAt,
        String status
) {
}
