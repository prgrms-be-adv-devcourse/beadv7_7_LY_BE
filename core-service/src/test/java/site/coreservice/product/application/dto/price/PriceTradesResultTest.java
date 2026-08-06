package site.coreservice.product.application.dto.price;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.coreservice.product.domain.price.ClosedAuction;
import site.coreservice.product.domain.price.MediaCondition;
import site.coreservice.product.domain.price.PriceHistory;

class PriceTradesResultTest {

    private static final LocalDateTime CLOSED_AT = LocalDateTime.of(2026, 7, 10, 20, 31);
    private static final LocalDateTime CONFIRMED_AT = LocalDateTime.of(2026, 7, 11, 10, 0);

    @Test
    @DisplayName("거래를 점 데이터(컨디션·가격·낙찰시각)로 옮긴다 — 확정시각은 응답에 없다")
    void of_점_데이터_매핑() {
        // given
        PriceHistory trade = PriceHistory.of(
                new ClosedAuction(1024L, 55L, MediaCondition.NEAR_MINT, 72000L, 7, CLOSED_AT, "ENDED_WON"),
                CONFIRMED_AT);

        // when
        PriceTradesResult result = PriceTradesResult.of(55L, List.of(trade));

        // then — 시간축은 낙찰시각(tradedAt)이다 (확정시각 confirmedAt 아님, D4 스키마 결정)
        assertThat(result.productId()).isEqualTo(55L);
        assertThat(result.trades()).hasSize(1);
        PriceTradesResult.Trade point = result.trades().get(0);
        assertThat(point.condition()).isEqualTo(MediaCondition.NEAR_MINT);
        assertThat(point.price()).isEqualTo(72000L);
        assertThat(point.tradedAt()).isEqualTo(CLOSED_AT);
    }

    @Test
    @DisplayName("거래가 없으면 빈 목록을 담는다")
    void of_빈_거래면_빈_목록() {
        // given-when
        PriceTradesResult result = PriceTradesResult.of(55L, List.of());

        // then
        assertThat(result.trades()).isEmpty();
    }
}
