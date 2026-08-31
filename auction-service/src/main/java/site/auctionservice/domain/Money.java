package site.auctionservice.domain;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.auctionservice.exception.AuctionErrorCode;
import site.auctionservice.exception.AuctionException;

import java.math.BigDecimal;
import java.util.Objects;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Money {
    private BigDecimal value;

    private Money(BigDecimal value) {
        this.value = value;
    }

    public static Money from(BigDecimal value) {
        Objects.requireNonNull(value, "금액은 null일 수 없습니다.");
        if (value.compareTo(BigDecimal.ZERO) < 0)
            throw new AuctionException(AuctionErrorCode.MONEY_AMOUNT_NEGATIVE);
        return new Money(value);
    }

    public static Money of(long value) {
        if (value < 0)
            throw new AuctionException(AuctionErrorCode.MONEY_AMOUNT_NEGATIVE);
        return new Money(BigDecimal.valueOf(value));
    }

    public Money plus(Money other) {
        return Money.from(this.value.add(other.value));
    }

    public Money minus(Money other) {
        return Money.from(this.value.subtract(other.value));
    }

    public boolean isGreaterThanOrEqual(Money other) {
        return this.value.compareTo(other.value) >= 0;
    }

    public boolean isGreaterThan(Money other)    { return this.value.compareTo(other.value) > 0; }

    public boolean isLessThanOrEqual(Money other) {
        return this.value.compareTo(other.value) <= 0;
    }

    public boolean isLessThan(Money other)       { return this.value.compareTo(other.value) < 0; }

    public boolean isSameAmount(Money other) {
        return this.value.compareTo(other.value) == 0;
    }

    /** 배수 검증용 */
    public boolean isMultipleOf(Money unit) {
        return this.value.remainder(unit.value).compareTo(BigDecimal.ZERO) == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money m)) return false;
        return this.value.compareTo(m.value) == 0;
    }

    @Override
    public int hashCode() {
        return value.stripTrailingZeros().hashCode();
    }
}
