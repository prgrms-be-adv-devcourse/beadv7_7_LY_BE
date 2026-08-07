package site.fulfillmentservice.settlement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Embeddable
public class Money {

    private static final int SCALE = 0; // 원화 기준, 소수점 없음.

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

    public static Money zero() {
        return new Money(BigDecimal.ZERO);
    }

    /** 배치 정산 총액 합산 (totalAmount = sum of netAmount) */
    public Money add(Money other) {
        return new Money(this.value.add(other.value));
    }

    /** netAmount = finalBidPrice - commissionAmount */
    public Money subtract(Money other) {
        return new Money(this.value.subtract(other.value));
    }

    /** commissionAmount = finalBidPrice * commissionRate */
    public Money multiply(BigDecimal factor) {
        return new Money(this.value.multiply(factor).setScale(SCALE, RoundingMode.DOWN));
    }

    public BigDecimal getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Money)) {
            return false;
        }
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
