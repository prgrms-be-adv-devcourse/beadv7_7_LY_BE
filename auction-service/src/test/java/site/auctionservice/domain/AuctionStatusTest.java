package site.auctionservice.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

class AuctionStatusTest {

    @Test
    void testScheduled_canTransitToRunningOrCanceled() {
        // then
        assertThat(AuctionStatus.SCHEDULED.canTransitTo(AuctionStatus.RUNNING)).isTrue();
        assertThat(AuctionStatus.SCHEDULED.canTransitTo(AuctionStatus.CANCELED)).isTrue();
        assertThat(AuctionStatus.SCHEDULED.canTransitTo(AuctionStatus.ENDED_WON)).isFalse();
        assertThat(AuctionStatus.SCHEDULED.canTransitTo(AuctionStatus.ENDED_FAILED)).isFalse();
    }

    @Test
    void testRunning_canTransitToEndedWonOrEndedFailed() {
        // then
        assertThat(AuctionStatus.RUNNING.canTransitTo(AuctionStatus.ENDED_WON)).isTrue();
        assertThat(AuctionStatus.RUNNING.canTransitTo(AuctionStatus.ENDED_FAILED)).isTrue();
        assertThat(AuctionStatus.RUNNING.canTransitTo(AuctionStatus.SCHEDULED)).isFalse();
        assertThat(AuctionStatus.RUNNING.canTransitTo(AuctionStatus.CANCELED)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = AuctionStatus.class, names = {"ENDED_WON", "ENDED_FAILED", "CANCELED"})
    void testTerminalStatuses_cannotTransitToAnything(AuctionStatus terminal) {
        // then
        for (AuctionStatus next : EnumSet.allOf(AuctionStatus.class)) {
            assertThat(terminal.canTransitTo(next)).isFalse();
        }
    }
}
