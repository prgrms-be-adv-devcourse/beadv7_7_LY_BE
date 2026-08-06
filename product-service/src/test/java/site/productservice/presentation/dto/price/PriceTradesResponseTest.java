package site.productservice.presentation.dto.price;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.productservice.application.dto.price.PriceTradesResult;
import site.productservice.domain.price.MediaCondition;

class PriceTradesResponseTest {

    @Test
    @DisplayName("거래 점 데이터를 컨디션 문자열과 함께 그대로 옮긴다")
    void from_점_데이터_매핑() {
        // given
        LocalDateTime tradedAt = LocalDateTime.of(2026, 7, 20, 14, 0);
        PriceTradesResult result = new PriceTradesResult(42L, List.of(
                new PriceTradesResult.Trade(MediaCondition.NEAR_MINT, 85000L, tradedAt)));

        // when
        PriceTradesResponse response = PriceTradesResponse.from(result);

        // then
        assertThat(response.productId()).isEqualTo(42L);
        PriceTradesResponse.TradePoint point = response.trades().get(0);
        assertThat(point.condition()).isEqualTo("NEAR_MINT");
        assertThat(point.price()).isEqualTo(85000L);
        assertThat(point.tradedAt()).isEqualTo(tradedAt);
    }

    @Test
    @DisplayName("빈 거래면 빈 배열로 내린다")
    void from_빈_거래면_빈_배열() {
        // given-when
        PriceTradesResponse response = PriceTradesResponse.from(new PriceTradesResult(42L, List.of()));

        // then
        assertThat(response.trades()).isEmpty();
    }
}
