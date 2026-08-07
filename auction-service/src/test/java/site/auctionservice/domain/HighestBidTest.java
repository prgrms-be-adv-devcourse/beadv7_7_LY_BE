package site.auctionservice.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HighestBidTest {

    @Test
    @DisplayName("필수 값이 null이면 예외가 발생한다")
    void testOf_nullFields_throws() {
        // when & then
        assertThatThrownBy(() -> HighestBid.of(null, 1L, 1L)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> HighestBid.of(Money.of(1_000L), null, 1L)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> HighestBid.of(Money.of(1_000L), 1L, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("최고입찰자 여부를 판별한다")
    void testIsBidder_checksBidderId() {
        // given
        HighestBid highestBid = HighestBid.of(Money.of(1_000L), 42L, 1L);

        // then
        assertThat(highestBid.isBidder(42L)).isTrue();
        assertThat(highestBid.isBidder(43L)).isFalse();
    }
}
