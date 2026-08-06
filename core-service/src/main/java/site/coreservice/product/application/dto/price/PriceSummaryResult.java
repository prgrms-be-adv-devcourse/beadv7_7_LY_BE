package site.coreservice.product.application.dto.price;

import java.util.List;
import site.coreservice.product.domain.price.ConditionPriceStat;
import site.coreservice.product.domain.price.MediaCondition;

/**
 * 시세 요약 결과(스펙 3-1). 평균은 여기서 원 단위 반올림한 정수가 된다 — 도메인 통계는
 * 소수 원본을 유지한다.
 */
public record PriceSummaryResult(Long productId, List<ConditionStat> conditions) {

    public static PriceSummaryResult of(Long productId, List<ConditionPriceStat> stats) {
        List<ConditionStat> conditions = stats.stream()
                .map(stat -> new ConditionStat(stat.condition(), stat.sampleCount(),
                        Math.round(stat.averagePrice()), stat.lowestPrice(),
                        stat.highestPrice()))
                .toList();
        return new PriceSummaryResult(productId, conditions);
    }

    public record ConditionStat(MediaCondition condition, long sampleCount, long averagePrice,
            long lowestPrice, long highestPrice) {
    }
}
