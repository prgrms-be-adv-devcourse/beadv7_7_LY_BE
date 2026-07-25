package site.coreservice.product.application.dto;

import java.time.LocalDateTime;
import java.util.List;
import site.coreservice.product.domain.MediaCondition;
import site.coreservice.product.domain.PriceHistory;

/**
 * 시세 추이 결과(스펙 3-2). 거래 1건 = 차트의 점 1개. 시간축은 낙찰시각(tradedAt)이다.
 */
public record PriceTradesResult(Long productId, List<Trade> trades) {

    public static PriceTradesResult of(Long productId, List<PriceHistory> priceHistories) {
        List<Trade> trades = priceHistories.stream()
                .map(history -> new Trade(history.getMediaCondition(), history.getFinalPrice(),
                        history.getTradedAt()))
                .toList();
        return new PriceTradesResult(productId, trades);
    }

    public record Trade(MediaCondition condition, long price, LocalDateTime tradedAt) {
    }
}
