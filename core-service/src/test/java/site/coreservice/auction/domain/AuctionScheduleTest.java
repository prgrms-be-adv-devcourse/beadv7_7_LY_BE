package site.coreservice.auction.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuctionScheduleTest {

    private final Period period = Period.from(
            LocalDateTime.of(2026, 7, 1, 0, 0),
            LocalDateTime.of(2026, 7, 2, 0, 0)
    );

    @Test
    @DisplayName("연장이 비활성화되면 연장 시간이 전달되어도 null로 정규화된다")
    void testFrom_extensionDisabled_normalizesExtensionTimeToNull() {
        // given
        Integer extensionTime = 10;

        // when
        AuctionSchedule schedule = AuctionSchedule.from(period, false, extensionTime);

        // then
        assertThat(schedule.getExtensionTime()).isNull();
    }

    @Test
    @DisplayName("연장이 활성화되었는데 연장 시간이 없으면 예외가 발생한다")
    void testFrom_extensionEnabledWithoutTime_throws() {
        // when & then
        assertThatThrownBy(() -> AuctionSchedule.from(period, true, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("연장 시간이 최소값 미만이면 예외가 발생한다")
    void testFrom_extensionTimeBelowMinimum_throws() {
        // given
        int belowMinExtensionTime = 0;

        // when & then
        assertThatThrownBy(() -> AuctionSchedule.from(period, true, belowMinExtensionTime)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("경매 기간이 null이면 예외가 발생한다")
    void testFrom_nullPeriod_throws() {
        // when & then
        assertThatThrownBy(() -> AuctionSchedule.from(null, false, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("입찰 가능 여부는 내부 기간에 위임한다")
    void testIsBiddableAt_delegatesToPeriod() {
        // given
        AuctionSchedule schedule = AuctionSchedule.from(period, false, null);

        // then
        assertThat(schedule.isBiddableAt(period.getStartAt())).isTrue();
        assertThat(schedule.isBiddableAt(period.getEndAt())).isFalse();
    }

    @Test
    @DisplayName("시작/종료 여부는 내부 기간에 위임한다")
    void testIsStartedAndEnded_delegateToPeriod() {
        // given
        AuctionSchedule schedule = AuctionSchedule.from(period, false, null);

        // then
        assertThat(schedule.isStartedAt(period.getStartAt())).isTrue();
        assertThat(schedule.isEndedAt(period.getEndAt())).isTrue();
        assertThat(schedule.isEndedAt(period.getEndAt().minusSeconds(1))).isFalse();
    }
}
