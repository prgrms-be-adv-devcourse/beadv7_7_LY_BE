package site.productservice.presentation.dto.price;

import java.util.List;
import site.productservice.application.dto.price.PriceSummaryResult;

/** 시세 요약 응답(스펙 3-1). 컨디션은 풀네임 문자열 — 축약 표기(M/NM)는 화면 표시 계층의 몫. */
public record PriceSummaryResponse(Long productId, List<ConditionSummary> conditions) {

    public static PriceSummaryResponse from(PriceSummaryResult result) {
        List<ConditionSummary> conditions = result.conditions().stream()
                .map(stat -> new ConditionSummary(stat.condition().name(), stat.sampleCount(),
                        stat.averagePrice(), stat.lowestPrice(), stat.highestPrice()))
                .toList();
        return new PriceSummaryResponse(result.productId(), conditions);
    }

    public record ConditionSummary(String condition, long sampleCount, long averagePrice, long lowestPrice,
            long highestPrice) {
    }
}
