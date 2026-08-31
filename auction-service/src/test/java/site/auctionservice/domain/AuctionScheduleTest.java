package site.auctionservice.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.auctionservice.exception.AuctionErrorCode;
import site.auctionservice.exception.AuctionException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuctionScheduleTest {

    private final Period period = Period.of(
            LocalDateTime.of(2026, 7, 1, 0, 0),
            LocalDateTime.of(2026, 7, 2, 0, 0)
    );

    @Test
    @DisplayName("연장이 비활성화되면 연장 시간이 전달되어도 null로 정규화된다")
    void testOf_extensionDisabled_normalizesExtensionTimeToNull() {
        // given
        Integer extensionTime = 10;

        // when
        AuctionSchedule schedule = AuctionSchedule.of(period, false, extensionTime);

        // then
        assertThat(schedule.getExtensionTime()).isNull();
    }

    @Test
    @DisplayName("연장이 활성화되었는데 연장 시간이 없으면 예외가 발생한다")
    void testOf_extensionEnabledWithoutTime_throws() {
        // when & then
        assertThatThrownBy(() -> AuctionSchedule.of(period, true, null))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.EXTENSION_TIME_TOO_SHORT);
    }

    @Test
    @DisplayName("연장 시간이 최소값 미만이면 예외가 발생한다")
    void testOf_extensionTimeBelowMinimum_throws() {
        // given
        int belowMinExtensionTime = 0;

        // when & then
        assertThatThrownBy(() -> AuctionSchedule.of(period, true, belowMinExtensionTime))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.EXTENSION_TIME_TOO_SHORT);
    }

    @Test
    @DisplayName("연장 시간이 최대값을 초과하면 예외가 발생한다")
    void testOf_extensionTimeAboveMaximum_throws() {
        // given
        int aboveMaxExtensionTime = AuctionPolicy.MAX_EXTENSION_MINUTES + 1;

        // when & then
        assertThatThrownBy(() -> AuctionSchedule.of(period, true, aboveMaxExtensionTime))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.EXTENSION_TIME_TOO_LONG);
    }

    @Test
    @DisplayName("경매 기간이 null이면 예외가 발생한다")
    void testOf_nullPeriod_throws() {
        // when & then
        assertThatThrownBy(() -> AuctionSchedule.of(null, false, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("입찰 가능 여부는 내부 기간에 위임한다")
    void testIsBiddableAt_delegatesToPeriod() {
        // given
        AuctionSchedule schedule = AuctionSchedule.of(period, false, null);

        // then
        assertThat(schedule.isBiddableAt(period.getStartAt())).isTrue();
        assertThat(schedule.isBiddableAt(period.getEndAt())).isTrue();
    }

    @Test
    @DisplayName("시작/종료 여부는 내부 기간에 위임한다")
    void testIsStartedAndEnded_delegateToPeriod() {
        // given
        AuctionSchedule schedule = AuctionSchedule.of(period, false, null);

        // then
        assertThat(schedule.isStartedAt(period.getStartAt())).isTrue();
        assertThat(schedule.isEndedAt(period.getEndAt())).isTrue();
        assertThat(schedule.isEndedAt(period.getEndAt().minusSeconds(1))).isFalse();
    }

    @Test
    @DisplayName("연장이 비활성화되어 있으면 마감 임박이어도 연장하지 않는다")
    void testExtendIfNeeded_extensionDisabled_returnsSameSchedule() {
        // given
        AuctionSchedule schedule = AuctionSchedule.of(period, false, null);

        // when
        AuctionSchedule result = schedule.extendIfNeeded(period.getEndAt().minusMinutes(1));

        // then
        assertThat(result).isEqualTo(schedule);
        assertThat(result.getPeriod().getEndAt()).isEqualTo(period.getEndAt());
    }

    @Test
    @DisplayName("연장이 활성화되어 있어도 마감 임박이 아니면 연장하지 않는다")
    void testExtendIfNeeded_notNearEnd_returnsSameSchedule() {
        // given
        AuctionSchedule schedule = AuctionSchedule.of(period, true, 10);

        // when
        AuctionSchedule result = schedule.extendIfNeeded(period.getEndAt().minusMinutes(30));

        // then
        assertThat(result.getPeriod().getEndAt()).isEqualTo(period.getEndAt());
    }

    @Test
    @DisplayName("연장이 활성화되어 있고 마감 임박이면 연장 시간만큼 종료 시각을 뒤로 민다")
    void testExtendIfNeeded_nearEnd_extendsEndAt() {
        // given
        AuctionSchedule schedule = AuctionSchedule.of(period, true, 10);

        // when
        AuctionSchedule result = schedule.extendIfNeeded(period.getEndAt().minusMinutes(1));

        // then
        assertThat(result.getPeriod().getEndAt()).isEqualTo(period.getEndAt().plusMinutes(10));
        assertThat(result.getPeriod().getStartAt()).isEqualTo(period.getStartAt());
    }

    @Test
    @DisplayName("연장 횟수가 최대치에 도달하면 마감 임박이어도 더 이상 연장하지 않는다")
    void testExtendIfNeeded_maxExtensionCountReached_stopsExtending() {
        // given
        AuctionSchedule schedule = AuctionSchedule.of(period, true, 10);
        for (int i = 0; i < AuctionPolicy.MAX_EXTENSION_COUNT; i++) {
            schedule = schedule.extendIfNeeded(schedule.getPeriod().getEndAt().minusMinutes(1));
        }
        LocalDateTime endAtAfterMaxExtensions = schedule.getPeriod().getEndAt();

        // when
        AuctionSchedule result = schedule.extendIfNeeded(schedule.getPeriod().getEndAt().minusMinutes(1));

        // then
        assertThat(result.getPeriod().getEndAt()).isEqualTo(endAtAfterMaxExtensions);
    }
}
