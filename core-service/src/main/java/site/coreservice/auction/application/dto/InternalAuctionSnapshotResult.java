package site.coreservice.auction.application.dto;

import site.coreservice.auction.domain.Auction;
import site.coreservice.auction.domain.AuctionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 다른 애그리거트(CartItem 등)가 경매 정보 일부만 필요할 때 쓰는 요약 DTO
public record InternalAuctionSnapshotResult(
    Long id,
    AuctionStatus status,
    BigDecimal currentPrice,
    LocalDateTime startAt,
    LocalDateTime endAt
) {

    public static InternalAuctionSnapshotResult from(Auction auction) {
        return new InternalAuctionSnapshotResult(
            auction.getId(),
            auction.getEffectiveStatusAt(LocalDateTime.now()),
            auction.getPricing().nextMinBidAmount(auction.getHighestBid()).getValue(),
            auction.getSchedule().getPeriod().getStartAt(),
            auction.getSchedule().getPeriod().getEndAt()
        );
    }
}
