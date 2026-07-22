package site.coreservice.auction.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    @DisplayName("음수 금액으로 생성하면 예외가 발생한다")
    void testFrom_negativeValue_throws() {
        // given
        BigDecimal negativeAmount = BigDecimal.valueOf(-1);

        // when & then
        assertThatThrownBy(() -> Money.from(negativeAmount)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("null 금액으로 생성하면 예외가 발생한다")
    void testFrom_nullValue_throws() {
        // when & then
        assertThatThrownBy(() -> Money.from(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("0원은 유효한 금액이다")
    void testFrom_zero_isValid() {
        // when
        Money money = Money.from(BigDecimal.ZERO);

        // then
        assertThat(money).isEqualTo(Money.of(0L));
    }

    @Test
    @DisplayName("두 금액을 더하면 합산된다")
    void testPlus_addsAmounts() {
        // given
        Money money = Money.of(1_000L);
        Money addAmount = Money.of(500L);

        // when
        Money result = money.plus(addAmount);

        // then
        assertThat(result).isEqualTo(Money.of(1_500L));
    }

    @Test
    @DisplayName("두 금액을 빼면 차감된다")
    void testMinus_subtractsAmounts() {
        // given
        Money money = Money.of(1_000L);
        Money subtractAmount = Money.of(300L);

        // when
        Money result = money.minus(subtractAmount);

        // then
        assertThat(result).isEqualTo(Money.of(700L));
    }

    @Test
    @DisplayName("빼기 결과가 음수가 되면 예외가 발생한다")
    void testMinus_negativeResult_throws() {
        // given
        Money money = Money.of(1_000L);
        Money subtractAmount = Money.of(1_500L);

        // when & then
        assertThatThrownBy(() -> money.minus(subtractAmount)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("크기 비교 연산이 정확하다")
    void testComparison_operations() {
        // given
        Money small = Money.of(1_000L);
        Money large = Money.of(2_000L);

        // then
        assertThat(large.isGreaterThan(small)).isTrue();
        assertThat(small.isGreaterThan(large)).isFalse();
        assertThat(large.isGreaterThanOrEqual(large)).isTrue();
        assertThat(small.isLessThan(large)).isTrue();
        assertThat(small.isLessThanOrEqual(small)).isTrue();
    }

    @Test
    @DisplayName("스케일이 달라도 값이 같으면 동일한 금액이다")
    void testIsSameAmount_ignoresScale() {
        // given
        Money withoutScale = Money.from(new BigDecimal("1000"));
        Money withScale = Money.from(new BigDecimal("1000.00"));

        // then
        assertThat(withoutScale.isSameAmount(withScale)).isTrue();
        assertThat(withoutScale).isEqualTo(withScale);
        assertThat(withoutScale.hashCode()).isEqualTo(withScale.hashCode());
    }

    @Test
    @DisplayName("단위 금액의 배수인지 판별한다")
    void testIsMultipleOf_checksMultiple() {
        // given
        Money amount = Money.of(1_050L);
        Money unit = Money.of(50L);
        Money notMultiple = Money.of(40L);

        // then
        assertThat(amount.isMultipleOf(unit)).isTrue();
        assertThat(amount.isMultipleOf(notMultiple)).isFalse();
    }
}
