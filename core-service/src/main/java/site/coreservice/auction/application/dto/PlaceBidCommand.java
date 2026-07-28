package site.coreservice.auction.application.dto;

import java.math.BigDecimal;

public record PlaceBidCommand(
        Long auctionId,
        Long bidderId,
        BigDecimal amount
) {
}
