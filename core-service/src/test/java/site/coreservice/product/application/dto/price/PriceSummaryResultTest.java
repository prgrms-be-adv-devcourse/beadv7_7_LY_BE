package site.coreservice.product.application.dto.price;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.coreservice.product.domain.price.ConditionPriceStat;
import site.coreservice.product.domain.price.MediaCondition;

class PriceSummaryResultTest {

    @Test
    @DisplayName("통계를 응답용으로 옮기며 평균을 원 단위 반올림한다 (.5는 올림)")
    void of_평균_원_단위_반올림() {
        // given
        List<ConditionPriceStat> stats = List.of(
                new ConditionPriceStat(MediaCondition.NEAR_MINT, 2L, 100.5, 100L, 101L),
                new ConditionPriceStat(MediaCondition.GOOD, 3L, 33333.3333, 30000L, 40000L));

        // when
        PriceSummaryResult result = PriceSummaryResult.of(42L, stats);

        // then — 100.5는 101로 올림(버림이면 100), 33333.3333은 33333
        assertThat(result.productId()).isEqualTo(42L);
        assertThat(result.conditions().get(0).averagePrice()).isEqualTo(101L);
        assertThat(result.conditions().get(1).averagePrice()).isEqualTo(33333L);
    }

    @Test
    @DisplayName("컨디션·건수·최저·최고는 그대로 옮긴다")
    void of_필드_매핑() {
        // given
        List<ConditionPriceStat> stats = List.of(
                new ConditionPriceStat(MediaCondition.MINT, 5L, 90000.0, 80000L, 120000L));

        // when
        PriceSummaryResult result = PriceSummaryResult.of(42L, stats);

        // then
        PriceSummaryResult.ConditionStat stat = result.conditions().get(0);
        assertThat(stat.condition()).isEqualTo(MediaCondition.MINT);
        assertThat(stat.sampleCount()).isEqualTo(5L);
        assertThat(stat.lowestPrice()).isEqualTo(80000L);
        assertThat(stat.highestPrice()).isEqualTo(120000L);
    }

    @Test
    @DisplayName("통계가 없으면 빈 목록을 담는다")
    void of_빈_통계면_빈_목록() {
        // given-when
        PriceSummaryResult result = PriceSummaryResult.of(42L, List.of());

        // then
        assertThat(result.conditions()).isEmpty();
    }
}
