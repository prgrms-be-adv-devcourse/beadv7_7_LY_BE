package site.coreservice.auction.application.dto;

import java.math.BigDecimal;
import java.util.List;

public sealed interface AuctionStatusDetail {

    record ScheduledDetail() implements AuctionStatusDetail {
    }

    record RunningDetail(BigDecimal highestBidAmount, BigDecimal nextMinBidAmount, long bidCount,
                         List<BidDetailResult> bidDetails, Boolean myHighest) implements AuctionStatusDetail {
    }

    record EndedWonDetail(BidDetailResult bidDetail) implements AuctionStatusDetail {
    }

    record EndedFailedDetail() implements AuctionStatusDetail {
    }

    // 종료 시각은 지났지만 종료 스케줄러가 아직 낙찰/유찰을 확정하지 않은 구간 => 상세 화면 조회 시 CLOSING 표시
    record ClosingDetail() implements AuctionStatusDetail {
    }
}
