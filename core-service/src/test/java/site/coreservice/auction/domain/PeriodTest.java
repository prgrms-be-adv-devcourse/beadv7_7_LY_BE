package site.coreservice.auction.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PeriodTest {

    private final LocalDateTime start = LocalDateTime.of(2026, 7, 1, 0, 0);
    private final LocalDateTime end = LocalDateTime.of(2026, 7, 2, 0, 0);

    @Test
    @DisplayName("최소 지속 시간(1시간) 미만이면 예외가 발생한다")
    void testFrom_tooShortDuration_throws() {
        // given
        LocalDateTime tooShortEnd = start.plusMinutes(59);

        // when & then
        assertThatThrownBy(() -> Period.from(start, tooShortEnd)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("정확히 최소 지속 시간이면 생성에 성공한다")
    void testFrom_exactlyMinDuration_succeeds() {
        // given
        LocalDateTime minDurationEnd = start.plusHours(1);

        // when
        Period period = Period.from(start, minDurationEnd);

        // then
        assertThat(period.getEndAt()).isEqualTo(minDurationEnd);
    }

    @Test
    @DisplayName("시작/종료 시각이 null이면 예외가 발생한다")
    void testFrom_nullDates_throws() {
        // when & then
        assertThatThrownBy(() -> Period.from(null, end)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Period.from(start, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("시작 시각 이상, 종료 시각 미만인 경우에만 포함된다")
    void testContains_checksHalfOpenRange() {
        // given
        Period period = Period.from(start, end);

        // then
        assertThat(period.contains(start)).isTrue();
        assertThat(period.contains(start.plusHours(1))).isTrue();
        assertThat(period.contains(end)).isFalse();
        assertThat(period.contains(start.minusSeconds(1))).isFalse();
    }

    @Test
    @DisplayName("시작 시각 이후인지 판별한다")
    void testIsStarted_checksStartBoundary() {
        // given
        Period period = Period.from(start, end);

        // then
        assertThat(period.isStarted(start)).isTrue();
        assertThat(period.isStarted(start.minusSeconds(1))).isFalse();
    }

    @Test
    @DisplayName("종료 시각 이후인지 판별한다")
    void testIsEnded_checksEndBoundary() {
        // given
        Period period = Period.from(start, end);

        // then
        assertThat(period.isEnded(end)).isTrue();
        assertThat(period.isEnded(end.minusSeconds(1))).isFalse();
    }
}
