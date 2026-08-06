package site.coreservice.product.presentation.dto.price;

import java.time.LocalDateTime;
import java.util.List;
import site.coreservice.product.application.dto.price.PriceTradesResult;

/** 시세 추이 응답(스펙 3-2). 최신순 최대 100건 — 거래 1건이 차트의 점 1개다. */
public record PriceTradesResponse(Long productId, List<TradePoint> trades) {

    public static PriceTradesResponse from(PriceTradesResult result) {
        List<TradePoint> trades = result.trades().stream()
                .map(trade -> new TradePoint(trade.condition().name(), trade.price(), trade.tradedAt()))
                .toList();
        return new PriceTradesResponse(result.productId(), trades);
    }

    public record TradePoint(String condition, long price, LocalDateTime tradedAt) {
    }
}
