package site.auctionservice.domain;

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
    void testOf_tooShortDuration_throws() {
        // given
        LocalDateTime tooShortEnd = start.plusMinutes(59);

        // when & then
        assertThatThrownBy(() -> Period.of(start, tooShortEnd)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("정확히 최소 지속 시간이면 생성에 성공한다")
    void testOf_exactlyMinDuration_succeeds() {
        // given
        LocalDateTime minDurationEnd = start.plusHours(1);

        // when
        Period period = Period.of(start, minDurationEnd);

        // then
        assertThat(period.getEndAt()).isEqualTo(minDurationEnd);
    }

    @Test
    @DisplayName("시작/종료 시각이 null이면 예외가 발생한다")
    void testOf_nullDates_throws() {
        // when & then
        assertThatThrownBy(() -> Period.of(null, end)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Period.of(start, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("시작 시각 이상, 종료 시각 이하인 경우에만 포함된다")
    void testContains_checksHalfOpenRange() {
        // given
        Period period = Period.of(start, end);

        // then
        assertThat(period.contains(start)).isTrue();
        assertThat(period.contains(start.plusHours(1))).isTrue();
        assertThat(period.contains(end)).isTrue();
        assertThat(period.contains(start.minusSeconds(1))).isFalse();
    }

    @Test
    @DisplayName("시작 시각 이후인지 판별한다")
    void testIsStarted_checksStartBoundary() {
        // given
        Period period = Period.of(start, end);

        // then
        assertThat(period.isStarted(start)).isTrue();
        assertThat(period.isStarted(start.minusSeconds(1))).isFalse();
    }

    @Test
    @DisplayName("종료 시각 이후인지 판별한다")
    void testIsEnded_checksEndBoundary() {
        // given
        Period period = Period.of(start, end);

        // then
        assertThat(period.isEnded(end)).isTrue();
        assertThat(period.isEnded(end.minusSeconds(1))).isFalse();
    }

    @Test
    @DisplayName("종료까지 N분 이내로 남았으면 마감 임박이다")
    void testIsNearEnd_withinThreshold_isTrue() {
        // given
        Period period = Period.of(start, end);

        // then
        assertThat(period.isNearEnd(end.minusMinutes(5), 10)).isTrue();
    }

    @Test
    @DisplayName("종료까지 N분보다 많이 남았으면 마감 임박이 아니다")
    void testIsNearEnd_beyondThreshold_isFalse() {
        // given
        Period period = Period.of(start, end);

        // then
        assertThat(period.isNearEnd(end.minusMinutes(30), 10)).isFalse();
    }

    @Test
    @DisplayName("이미 종료된 시각이면 마감 임박이 아니라 이미 종료로 취급한다")
    void testIsNearEnd_alreadyEnded_isFalse() {
        // given
        Period period = Period.of(start, end);

        // then
        assertThat(period.isNearEnd(end, 10)).isFalse();
        assertThat(period.isNearEnd(end.plusMinutes(1), 10)).isFalse();
    }

    @Test
    @DisplayName("extendEnd는 시작 시각은 유지한 채 종료 시각만 뒤로 민다")
    void testExtendEnd_pushesEndAtOnly() {
        // given
        Period period = Period.of(start, end);

        // when
        Period extended = period.extendEnd(10);

        // then
        assertThat(extended.getStartAt()).isEqualTo(start);
        assertThat(extended.getEndAt()).isEqualTo(end.plusMinutes(10));
    }
}
