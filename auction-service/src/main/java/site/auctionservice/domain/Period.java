package site.auctionservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.auctionservice.exception.AuctionErrorCode;
import site.auctionservice.exception.AuctionException;

import java.time.LocalDateTime;
import java.util.Objects;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Period {

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    private Period(LocalDateTime startAt, LocalDateTime endAt) {
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public static Period of(LocalDateTime startAt, LocalDateTime endAt) {
        Objects.requireNonNull(startAt, "시작 시각은 null일 수 없습니다.");
        Objects.requireNonNull(endAt, "종료 시각은 null일 수 없습니다.");
        if (startAt.plusHours(AuctionPolicy.MIN_DURATION_HOURS).isAfter(endAt)) {
            throw new AuctionException(AuctionErrorCode.AUCTION_DURATION_TOO_SHORT);
        }
        return new Period(startAt, endAt);
    }

    public boolean contains(LocalDateTime at) {
        return !at.isBefore(startAt) && !at.isAfter(endAt);
    }

    public boolean isStarted(LocalDateTime at) {
        return !at.isBefore(startAt);
    }

    public boolean isEnded(LocalDateTime at) {
        return !at.isBefore(endAt);
    }

    public boolean isNearEnd(LocalDateTime now, int minutes) {
        return now.isAfter(endAt.minusMinutes(minutes)) && !isEnded(now);
    }

    public Period extendEnd(int minutes) {
        return Period.of(startAt, endAt.plusMinutes(minutes));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Period p)) {
            return false;
        }
        return Objects.equals(startAt, p.startAt) && Objects.equals(endAt, p.endAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startAt, endAt);
    }
}
