package site.pointwalletservice.shared;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Embeddable
public class Money {

    private static final int SCALE = 0; // 원화 기준, 소수점 없음. 필요 시 조정.

    @Column(name = "amount", precision = 19, scale = SCALE)
    private final BigDecimal value;

    protected Money() {
        this.value = BigDecimal.ZERO;
    }

    private Money(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("금액은 null일 수 없습니다.");
        }
        BigDecimal normalized = value.setScale(SCALE, RoundingMode.UNNECESSARY);
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("금액은 음수일 수 없습니다.");
        }
        this.value = normalized;
    }

    public static Money of(BigDecimal value) {
        return new Money(value);
    }

    public static Money of(long value) {
        return new Money(BigDecimal.valueOf(value));
    }

    public static Money of(String value) {
        return new Money(new BigDecimal(value));
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO);
    }

    public Money add(Money other) {
        return new Money(this.value.add(other.value));
    }

    public Money subtract(Money other) {
        BigDecimal result = this.value.subtract(other.value);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("차감 후 금액이 음수가 될 수 없습니다. (잔액 부족)");
        }
        return new Money(result);
    }

    /** 수수료율 등 곱셈이 필요한 경우 (예: 환불수수료 = 금액 * 요율) */
    public Money multiply(BigDecimal factor) {
        return new Money(this.value.multiply(factor).setScale(SCALE, RoundingMode.DOWN));
    }

    public boolean isGreaterThanOrEqual(Money other) {
        return this.value.compareTo(other.value) >= 0;
    }

    public boolean isLessThan(Money other) {
        return this.value.compareTo(other.value) < 0;
    }

    public BigDecimal getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money)) return false;
        Money money = (Money) o;
        return value.compareTo(money.value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value.stripTrailingZeros());
    }

    @Override
    public String toString() {
        return value.toPlainString();
    }
}