package site.auctionservice.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BidTest {

    private final LocalDateTime now = LocalDateTime.of(2026, 7, 1, 12, 0);

    @Test
    @DisplayName("place()로 생성하면 ACTIVE 상태다")
    void testPlace_createsActiveBid() {
        // when
        Bid bid = Bid.place(1L, 2L, Money.of(1_000L), now);

        // then
        assertThat(bid.getOutcome()).isEqualTo(BidOutcome.ACTIVE);
        assertThat(bid.getAmount()).isEqualTo(Money.of(1_000L));
    }

    @Test
    @DisplayName("필수 값이 null이면 place()는 예외를 던진다")
    void testPlace_nullRequiredFields_throws() {
        // when & then
        assertThatThrownBy(() -> Bid.place(null, 2L, Money.of(1_000L), now)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Bid.place(1L, null, Money.of(1_000L), now)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Bid.place(1L, 2L, null, now)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Bid.place(1L, 2L, Money.of(1_000L), null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("markOutbid()는 ACTIVE 입찰을 OUTBID로 전이한다")
    void testMarkOutbid_fromActive_succeeds() {
        // given
        Bid bid = Bid.place(1L, 2L, Money.of(1_000L), now);

        // when
        bid.markOutbid();

        // then
        assertThat(bid.getOutcome()).isEqualTo(BidOutcome.OUTBID);
    }

    @Test
    @DisplayName("markWon()은 ACTIVE 입찰을 WON으로 전이한다")
    void testMarkWon_fromActive_succeeds() {
        // given
        Bid bid = Bid.place(1L, 2L, Money.of(1_000L), now);

        // when
        bid.markWon();

        // then
        assertThat(bid.getOutcome()).isEqualTo(BidOutcome.WON);
    }

    @Test
    @DisplayName("markCanceled()는 ACTIVE 입찰을 CANCELED로 전이한다")
    void testMarkCanceled_fromActive_succeeds() {
        // given
        Bid bid = Bid.place(1L, 2L, Money.of(1_000L), now);

        // when
        bid.markCanceled();

        // then
        assertThat(bid.getOutcome()).isEqualTo(BidOutcome.CANCELED);
    }

    @Test
    @DisplayName("이미 OUTBID된 입찰은 WON으로 전이할 수 없다")
    void testMarkWon_afterOutbid_throws() {
        // given
        Bid bid = Bid.place(1L, 2L, Money.of(1_000L), now);
        bid.markOutbid();

        // when & then
        assertThatThrownBy(bid::markWon).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("이미 WON인 입찰은 다시 OUTBID로 전이할 수 없다")
    void testMarkOutbid_afterWon_throws() {
        // given
        Bid bid = Bid.place(1L, 2L, Money.of(1_000L), now);
        bid.markWon();

        // when & then
        assertThatThrownBy(bid::markOutbid).isInstanceOf(IllegalStateException.class);
    }
}
