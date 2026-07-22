package site.coreservice.auction.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PricingTest {

    @Test
    @DisplayName("최소 시작가 미만이면 예외가 발생한다")
    void testFrom_startPriceBelowMinimum_throws() {
        // given
        Money belowMinStartPrice = Money.of(999L);

        // when & then
        assertThatThrownBy(() -> Pricing.from(belowMinStartPrice, Money.of(10L), Money.of(0L))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("최소 입찰 단위 미만이면 예외가 발생한다")
    void testFrom_bidUnitBelowMinimum_throws() {
        // given
        Money belowMinBidUnit = Money.of(9L);

        // when & then
        assertThatThrownBy(() -> Pricing.from(Money.of(1_000L), belowMinBidUnit, Money.of(0L))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("경계값(최소 시작가, 최소 입찰 단위)은 유효하다")
    void testFrom_boundaryValues_succeeds() {
        // given
        Money minStartPrice = Money.of(1_000L);
        Money minBidUnit = Money.of(10L);

        // when
        Pricing pricing = Pricing.from(minStartPrice, minBidUnit, Money.of(0L));

        // then
        assertThat(pricing.getStartPrice()).isEqualTo(minStartPrice);
        assertThat(pricing.getBidUnit()).isEqualTo(minBidUnit);
    }

    @Test
    @DisplayName("필수 값이 null이면 예외가 발생한다")
    void testFrom_nullFields_throws() {
        // when & then
        assertThatThrownBy(() -> Pricing.from(null, Money.of(10L), Money.of(0L))).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Pricing.from(Money.of(1_000L), null, Money.of(0L))).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Pricing.from(Money.of(1_000L), Money.of(10L), null)).isInstanceOf(NullPointerException.class);
    }
}
