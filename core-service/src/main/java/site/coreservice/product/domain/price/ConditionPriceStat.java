package site.coreservice.product.domain.price;

import java.util.Arrays;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 한 상품의 컨디션별 시세 통계 (표본 수·평균·최저·최고). 시세 요약 API의 계산 결과다.
 * <p>
 * 입력은 "최근 거래" 목록이고, 여기서 컨디션별로 묶어 통계를 낸다 — 시세 요약과 추이 차트가
 * 똑같은 거래 목록을 읽게 하기 위해 통계 계산을 DB가 아닌 여기서 한다 (스펙 결정 ④).
 * 평균은 소수 그대로 보존한다 — 원 단위 반올림은 응답으로 바꾸는 쪽(application)의 표현 정책.
 */
public record ConditionPriceStat(MediaCondition condition, long sampleCount, double averagePrice,
        long lowestPrice, long highestPrice) {

    /** 거래 목록을 컨디션별로 묶어 통계 리스트로 만든다. 순서는 컨디션 enum 선언 순(MINT→POOR)이다. */
    public static List<ConditionPriceStat> listFrom(List<PriceHistory> trades) {
        Map<MediaCondition, List<PriceHistory>> grouped = trades.stream()
                .collect(Collectors.groupingBy(PriceHistory::getMediaCondition));
        return Arrays.stream(MediaCondition.values())
                .filter(grouped::containsKey)
                .map(condition -> summarize(condition, grouped.get(condition)))
                .toList();
    }

    private static ConditionPriceStat summarize(MediaCondition condition, List<PriceHistory> trades) {
        LongSummaryStatistics statistics = trades.stream()
                .mapToLong(PriceHistory::getFinalPrice)
                .summaryStatistics();
        return new ConditionPriceStat(condition, statistics.getCount(), statistics.getAverage(),
                statistics.getMin(), statistics.getMax());
    }
}
