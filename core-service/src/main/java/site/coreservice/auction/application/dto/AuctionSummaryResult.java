package site.coreservice.auction.application.dto;

import site.coreservice.auction.domain.Auction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 다른 애그리거트(CartItem 등)가 경매 정보 일부만 필요할 때 쓰는 요약 DTO. AuctionResult와 별개로 유지한다.
public record AuctionSummaryResult(
        Long id,
        String status,
        BigDecimal currentPrice,
        LocalDateTime startAt,
        LocalDateTime endAt
) {
    public static AuctionSummaryResult from(Auction auction) {
        BigDecimal currentPrice = auction.hasBid()
                ? auction.getHighestBid().getAmount().getValue()
                : auction.getPricing().getStartPrice().getValue();

        return new AuctionSummaryResult(
                auction.getId(),
                auction.getStatus().name(),
                currentPrice,
                auction.getSchedule().getPeriod().getStartAt(),
                auction.getSchedule().getPeriod().getEndAt()
        );
    }
}
