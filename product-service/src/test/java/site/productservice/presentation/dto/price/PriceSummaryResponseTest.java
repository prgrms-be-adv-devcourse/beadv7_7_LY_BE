package site.productservice.presentation.dto.price;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.productservice.application.dto.price.PriceSummaryResult;
import site.productservice.domain.price.MediaCondition;

class PriceSummaryResponseTest {

    @Test
    @DisplayName("컨디션을 풀네임 문자열로 내리고 통계 필드를 그대로 옮긴다")
    void from_컨디션_문자열_변환과_필드_매핑() {
        // given
        PriceSummaryResult result = new PriceSummaryResult(42L, List.of(
                new PriceSummaryResult.ConditionStat(MediaCondition.VERY_GOOD_PLUS, 7L, 51000L, 40000L, 68000L)));

        // when
        PriceSummaryResponse response = PriceSummaryResponse.from(result);

        // then
        assertThat(response.productId()).isEqualTo(42L);
        PriceSummaryResponse.ConditionSummary summary = response.conditions().get(0);
        assertThat(summary.condition()).isEqualTo("VERY_GOOD_PLUS");
        assertThat(summary.sampleCount()).isEqualTo(7L);
        assertThat(summary.averagePrice()).isEqualTo(51000L);
        assertThat(summary.lowestPrice()).isEqualTo(40000L);
        assertThat(summary.highestPrice()).isEqualTo(68000L);
    }

    @Test
    @DisplayName("빈 통계면 빈 배열로 내린다 (거래 없음 = 정상 응답)")
    void from_빈_통계면_빈_배열() {
        // given-when
        PriceSummaryResponse response = PriceSummaryResponse.from(new PriceSummaryResult(42L, List.of()));

        // then
        assertThat(response.conditions()).isEmpty();
    }
}
