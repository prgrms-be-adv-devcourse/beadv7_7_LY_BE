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
        Money belowMinStartPrice = Money.of(999L);

        // when & then
        assertThatThrownBy(() -> Pricing.of(belowMinStartPrice, Money.of(10L), Money.of(0L))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("최소 입찰 단위 미만이면 예외가 발생한다")
    void testOf_bidUnitBelowMinimum_throws() {
        // given
        Money belowMinBidUnit = Money.of(9L);

        // when & then
        assertThatThrownBy(() -> Pricing.of(Money.of(1_000L), belowMinBidUnit, Money.of(0L))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("경계값(최소 시작가, 최소 입찰 단위)은 유효하다")
    void testOf_boundaryValues_succeeds() {
        // given
        Money minStartPrice = Money.of(1_000L);
        Money minBidUnit = Money.of(100L);
        // when
        Pricing pricing = Pricing.of(minStartPrice, minBidUnit, Money.of(0L));

        // then
        assertThat(pricing.getStartPrice()).isEqualTo(minStartPrice);
        assertThat(pricing.getBidUnit()).isEqualTo(minBidUnit);
    }

    @Test
    @DisplayName("필수 값이 null이면 예외가 발생한다")
    void testOf_nullFields_throws() {
        // when & then
        assertThatThrownBy(() -> Pricing.of(null, Money.of(10L), Money.of(0L))).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Pricing.of(Money.of(1_000L), null, Money.of(0L))).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Pricing.of(Money.of(1_000L), Money.of(10L), null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("입찰이 없으면 다음 최소 입찰가는 시작 입찰가(시작가+배송비)와 같다")
    void testNextMinBidAmount_noBid_equalsStartBidAmount() {
        // given
        Pricing pricing = Pricing.of(Money.of(10_000L), Money.of(500L), Money.of(3_000L));

        // when
        Money nextMinBid = pricing.nextMinBidAmount(null);

        // then
        assertThat(nextMinBid).isEqualTo(Money.of(13_000L));
    }

    @Test
    @DisplayName("입찰이 있으면 다음 최소 입찰가는 최고입찰가에 입찰단위를 더한 값이다")
    void testNextMinBidAmount_withBid_addsUnitToHighestBid() {
        // given
        Pricing pricing = Pricing.of(Money.of(10_000L), Money.of(500L), Money.of(3_000L));
        HighestBid highestBid = HighestBid.of(Money.of(12_000L), 1L, 1L);

        // when
        Money nextMinBid = pricing.nextMinBidAmount(highestBid);

        // then
        assertThat(nextMinBid).isEqualTo(Money.of(12_500L));
    }
}
