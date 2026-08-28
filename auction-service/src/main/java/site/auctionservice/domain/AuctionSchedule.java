package site.auctionservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuctionSchedule {

    @Embedded
    private Period period;

    @Column(name = "extension_enabled", nullable = false)
    private boolean extensionEnabled;

    @Column(name = "extension_time")
    private Integer extensionTime;

    @Column(name = "extension_count", nullable = false)
    private int extensionCount;

    private AuctionSchedule(Period period, boolean extensionEnabled, Integer extensionTime, int extensionCount) {
        this.period = period;
        this.extensionEnabled = extensionEnabled;
        this.extensionTime = extensionTime;
        this.extensionCount = extensionCount;
    }

    public static AuctionSchedule of(Period period, boolean extensionEnabled,
        Integer extensionTime) {
        Objects.requireNonNull(period, "경매 기간은 null일 수 없습니다.");
        if (extensionEnabled) {
            if (extensionTime == null || extensionTime < AuctionPolicy.MIN_EXTENSION_MINUTES) {
                throw new IllegalArgumentException(
                    "연장 시간은 %d분 이상이어야 합니다.".formatted(AuctionPolicy.MIN_EXTENSION_MINUTES));
            }
            if (extensionTime > AuctionPolicy.MAX_EXTENSION_MINUTES) {
                throw new IllegalArgumentException(
                    "연장 시간은 %d분 이하여야 합니다.".formatted(AuctionPolicy.MAX_EXTENSION_MINUTES));
            }
        } else {
            extensionTime = null;
        }
        return new AuctionSchedule(period, extensionEnabled, extensionTime, 0);
    }

    public boolean isBiddableAt(LocalDateTime at) {
        return period.contains(at);
    }

    public boolean isEndedAt(LocalDateTime at) {
        return period.isEnded(at);
    }

    public boolean isStartedAt(LocalDateTime at) {
        return period.isStarted(at);
    }

    public AuctionSchedule extendIfNeeded(LocalDateTime now) {
        if (!extensionEnabled || extensionTime == null) {
            return this;
        }
        if (extensionCount >= AuctionPolicy.MAX_EXTENSION_COUNT) {
            return this;
        }
        if (!period.isNearEnd(now, extensionTime)) {
            return this;
        }
        return new AuctionSchedule(period.extendEnd(extensionTime), extensionEnabled, extensionTime, extensionCount + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuctionSchedule that)) {
            return false;
        }
        return extensionEnabled == that.extensionEnabled
            && extensionCount == that.extensionCount
            && Objects.equals(period, that.period)
            && Objects.equals(extensionTime, that.extensionTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(period, extensionEnabled, extensionTime, extensionCount);
    }

}
