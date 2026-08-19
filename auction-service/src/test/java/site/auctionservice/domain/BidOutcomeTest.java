package site.auctionservice.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

class BidOutcomeTest {

    @ParameterizedTest
    @EnumSource(value = BidOutcome.class, names = "ACTIVE")
    void testActive_canTransitToOutbidOrWonOrCanceled(BidOutcome active) {
        // then
        assertThat(active.canTransitTo(BidOutcome.OUTBID)).isTrue();
        assertThat(active.canTransitTo(BidOutcome.WON)).isTrue();
        assertThat(active.canTransitTo(BidOutcome.CANCELED)).isTrue();
        assertThat(active.canTransitTo(BidOutcome.ACTIVE)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = BidOutcome.class, names = {"OUTBID", "WON", "CANCELED"})
    void testTerminalOutcomes_cannotTransitToAnything(BidOutcome terminal) {
        // then
        for (BidOutcome next : EnumSet.allOf(BidOutcome.class)) {
            assertThat(terminal.canTransitTo(next)).isFalse();
        }
    }
}
