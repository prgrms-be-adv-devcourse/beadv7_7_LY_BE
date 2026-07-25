package site.coreservice.auction.application.dto;

import site.coreservice.auction.domain.Bid;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BidDetailResult(String bidderNickname, BigDecimal amount, LocalDateTime placedAt) {
    public static BidDetailResult of(Bid bid, String bidderNickname) {
        return new BidDetailResult(
                bidderNickname,
                bid.getAmount().getValue(),
                bid.getPlacedAt()
        );
    }
}
