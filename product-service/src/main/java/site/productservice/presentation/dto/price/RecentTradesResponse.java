package site.productservice.presentation.dto.price;

import java.time.LocalDateTime;
import java.util.List;
import site.productservice.application.dto.price.RecentTradesResult;

/** 전역 최근 낙찰 응답 — 홈 최근 낙찰 목록용. 최신순 최대 size건. */
public record RecentTradesResponse(List<RecentTrade> trades) {

    public static RecentTradesResponse from(RecentTradesResult result) {
        List<RecentTrade> trades = result.trades().stream()
                .map(trade -> new RecentTrade(trade.productId(), trade.title(), trade.artistName(),
                        trade.coverImageUrl(), trade.condition().name(), trade.price(), trade.tradedAt()))
                .toList();
        return new RecentTradesResponse(trades);
    }

    public record RecentTrade(Long productId, String title, String artistName, String coverImageUrl,
            String condition, long price, LocalDateTime tradedAt) {
    }
}
