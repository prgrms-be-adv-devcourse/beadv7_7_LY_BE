package site.auctionservice.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.auctionservice.exception.AuctionErrorCode;
import site.auctionservice.exception.AuctionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PricingTest {

    @Test
    @DisplayName("최소 시작가 미만이면 예외가 발생한다")
    void testOf_startPriceBelowMinimum_throws() {
        // given
        Money belowMinStartPrice = Money.of(999L);

        // when & then
        assertThatThrownBy(() -> Pricing.of(belowMinStartPrice, Money.of(10L), Money.of(0L)))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.START_PRICE_TOO_LOW);
    }

    @Test
    @DisplayName("최소 입찰 단위 미만이면 예외가 발생한다")
    void testOf_bidUnitBelowMinimum_throws() {
        // given
        Money belowMinBidUnit = Money.of(9L);

        // when & then
        assertThatThrownBy(() -> Pricing.of(Money.of(1_000L), belowMinBidUnit, Money.of(0L)))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.BID_UNIT_TOO_LOW);
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
    @DisplayName("입찰 단위가 최소 입찰 단위의 배수가 아니면 예외가 발생한다")
    void testOf_bidUnitNotMultipleOfMinimum_throws() {
        // given
        Money notMultipleOf100 = Money.of(150L);

        // when & then
        assertThatThrownBy(() -> Pricing.of(Money.of(10_000L), notMultipleOf100, Money.of(0L)))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.BID_UNIT_NOT_MULTIPLE_OF_MIN_UNIT);
    }

    @Test
    @DisplayName("입찰 단위가 시작가 이상이면 예외가 발생한다")
    void testOf_bidUnitNotLessThanStartPrice_throws() {
        // given
        Money startPrice = Money.of(1_000L);
        Money bidUnitEqualToStartPrice = Money.of(1_000L);

        // when & then
        assertThatThrownBy(() -> Pricing.of(startPrice, bidUnitEqualToStartPrice, Money.of(0L)))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.BID_UNIT_NOT_LESS_THAN_START_PRICE);
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

    @Test
    @DisplayName("시작 입찰가 기준 입찰 단위의 배수인 금액은 정렬된 것으로 판단한다")
    void testIsAlignedToBidUnit_multipleOfUnit_returnsTrue() {
        // given: 시작 입찰가 13_000(시작가 10_000 + 배송비 3_000), 입찰단위 500
        Pricing pricing = Pricing.of(Money.of(10_000L), Money.of(500L), Money.of(3_000L));

        // when & then
        assertThat(pricing.isAlignedToBidUnit(Money.of(13_000L))).isTrue();
        assertThat(pricing.isAlignedToBidUnit(Money.of(13_500L))).isTrue();
        assertThat(pricing.isAlignedToBidUnit(Money.of(14_000L))).isTrue();
    }

    @Test
    @DisplayName("시작 입찰가 기준 입찰 단위의 배수가 아닌 금액은 정렬되지 않은 것으로 판단한다")
    void testIsAlignedToBidUnit_notMultipleOfUnit_returnsFalse() {
        // given
        Pricing pricing = Pricing.of(Money.of(10_000L), Money.of(500L), Money.of(3_000L));

        // when & then
        assertThat(pricing.isAlignedToBidUnit(Money.of(13_100L))).isFalse();
    }

    @Test
    @DisplayName("시작 입찰가 미만인 금액은 정렬되지 않은 것으로 판단한다")
    void testIsAlignedToBidUnit_belowStartBidAmount_returnsFalse() {
        // given
        Pricing pricing = Pricing.of(Money.of(10_000L), Money.of(500L), Money.of(3_000L));

        // when & then
        assertThat(pricing.isAlignedToBidUnit(Money.of(12_999L))).isFalse();
    }
}
