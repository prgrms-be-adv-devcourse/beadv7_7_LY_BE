package site.coreservice.auction.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PricingTest {

    @Test
    @DisplayName("최소 시작가 미만이면 예외가 발생한다")
    void testOf_startPriceBelowMinimum_throws() {
        // given
        Money belowMinStartPrice = Money.from(999L);

        // when & then
        assertThatThrownBy(() -> Pricing.of(belowMinStartPrice, Money.from(10L), Money.from(0L))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("최소 입찰 단위 미만이면 예외가 발생한다")
    void testOf_bidUnitBelowMinimum_throws() {
        // given
        Money belowMinBidUnit = Money.from(9L);

        // when & then
        assertThatThrownBy(() -> Pricing.of(Money.from(1_000L), belowMinBidUnit, Money.from(0L))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("경계값(최소 시작가, 최소 입찰 단위)은 유효하다")
    void testOf_boundaryValues_succeeds() {
        // given
        Money minStartPrice = Money.from(1_000L);
        Money minBidUnit = Money.from(10L);

        // when
        Pricing pricing = Pricing.of(minStartPrice, minBidUnit, Money.from(0L));

        // then
        assertThat(pricing.getStartPrice()).isEqualTo(minStartPrice);
        assertThat(pricing.getBidUnit()).isEqualTo(minBidUnit);
    }

    @Test
    @DisplayName("필수 값이 null이면 예외가 발생한다")
    void testOf_nullFields_throws() {
        // when & then
        assertThatThrownBy(() -> Pricing.of(null, Money.from(10L), Money.from(0L))).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Pricing.of(Money.from(1_000L), null, Money.from(0L))).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Pricing.of(Money.from(1_000L), Money.from(10L), null)).isInstanceOf(NullPointerException.class);
    }
}
