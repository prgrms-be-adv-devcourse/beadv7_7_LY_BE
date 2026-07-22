package site.coreservice.auction.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Period {
    private static final int MIN_DURATION_HOURS = 1;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    private Period(LocalDateTime startAt, LocalDateTime endAt) {
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public static Period from(LocalDateTime startAt, LocalDateTime endAt) {
        Objects.requireNonNull(startAt, "시작 시각은 null일 수 없습니다.");
        Objects.requireNonNull(endAt, "종료 시각은 null일 수 없습니다.");
        if (startAt.plusHours(MIN_DURATION_HOURS).isAfter(endAt))
            throw new IllegalArgumentException("종료 시각은 시작 시각으로부터 최소 %d시간 이후여야 합니다.".formatted(MIN_DURATION_HOURS));
        return new Period(startAt, endAt);
    }

    public boolean contains(LocalDateTime at) {
        return !at.isBefore(startAt) && at.isBefore(endAt);
    }

    public boolean isStarted(LocalDateTime at) {
        return !at.isBefore(startAt);
    }

    public boolean isEnded(LocalDateTime at) {
        return !at.isBefore(endAt);
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Period p)) return false;
        return Objects.equals(startAt, p.startAt) && Objects.equals(endAt, p.endAt);
    }

    @Override public int hashCode() {
        return Objects.hash(startAt, endAt);
    }
}
