package site.coreservice.auction.domain;

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

    private AuctionSchedule(Period period, boolean extensionEnabled, Integer extensionTime) {
        this.period = period;
        this.extensionEnabled = extensionEnabled;
        this.extensionTime = extensionTime;
    }

    public static AuctionSchedule of(Period period, boolean extensionEnabled,
        Integer extensionTime) {
        Objects.requireNonNull(period, "경매 기간은 null일 수 없습니다.");
        if (extensionEnabled) {
            if (extensionTime == null || extensionTime < AuctionPolicy.MIN_EXTENSION_MINUTES) {
                throw new IllegalArgumentException(
                    "연장 시간은 %d분 이상이어야 합니다.".formatted(AuctionPolicy.MIN_EXTENSION_MINUTES));
            }
        } else {
            extensionTime = null;
        }
        return new AuctionSchedule(period, extensionEnabled, extensionTime);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuctionSchedule that)) {
            return false;
        }
        return extensionEnabled == that.extensionEnabled
            && Objects.equals(period, that.period)
            && Objects.equals(extensionTime, that.extensionTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(period, extensionEnabled, extensionTime);
    }

}
