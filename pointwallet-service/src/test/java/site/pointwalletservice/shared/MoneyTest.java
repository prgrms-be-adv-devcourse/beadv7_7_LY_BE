package site.pointwalletservice.shared;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Money VO")
class MoneyTest {

    @Nested
    @DisplayName("생성")
    class Creation {

        @Test
        @DisplayName("양수 금액으로 생성할 수 있다")
        void createWithPositiveValue() {
            Money money = Money.of(10_000);

            assertThat(money.getValue()).isEqualByComparingTo(BigDecimal.valueOf(10_000));
        }

        @Test
        @DisplayName("0으로 생성할 수 있다")
        void createWithZero() {
            Money money = Money.of(0);

            assertThat(money.getValue()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("zero()는 0원을 반환한다")
        void zeroFactory() {
            assertThat(Money.zero().getValue()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("음수로 생성하면 예외가 발생한다")
        void createWithNegativeValue_throwsException() {
            assertThatThrownBy(() -> Money.of(-1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("금액은 음수일 수 없습니다.");
        }

        @Test
        @DisplayName("null로 생성하면 예외가 발생한다")
        void createWithNull_throwsException() {
            assertThatThrownBy(() -> Money.of((BigDecimal) null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("금액은 null일 수 없습니다.");
        }

        @Test
        @DisplayName("문자열로 생성할 수 있다")
        void createWithString() {
            Money money = Money.of("15000");

            assertThat(money.getValue()).isEqualByComparingTo(BigDecimal.valueOf(15_000));
        }
    }

    @Nested
    @DisplayName("덧셈")
    class Add {

        @Test
        @DisplayName("두 금액을 더하면 합이 반환된다")
        void addReturnsSum() {
            Money a = Money.of(10_000);
            Money b = Money.of(5_000);

            Money result = a.add(b);

            assertThat(result.getValue()).isEqualByComparingTo(BigDecimal.valueOf(15_000));
        }

        @Test
        @DisplayName("원본 객체는 변경되지 않는다 (불변성)")
        void addDoesNotMutateOriginal() {
            Money a = Money.of(10_000);

            a.add(Money.of(5_000));

            assertThat(a.getValue()).isEqualByComparingTo(BigDecimal.valueOf(10_000));
        }
    }

    @Nested
    @DisplayName("뺄셈")
    class Subtract {

        @Test
        @DisplayName("잔액 범위 내에서 차감하면 차이가 반환된다")
        void subtractWithinBalance() {
            Money a = Money.of(10_000);
            Money b = Money.of(3_000);

            Money result = a.subtract(b);

            assertThat(result.getValue()).isEqualByComparingTo(BigDecimal.valueOf(7_000));
        }

        @Test
        @DisplayName("차감 결과가 정확히 0이면 허용된다")
        void subtractToExactlyZero() {
            Money a = Money.of(10_000);

            Money result = a.subtract(Money.of(10_000));

            assertThat(result.getValue()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("보유 금액보다 큰 금액을 차감하면 예외가 발생한다")
        void subtractMoreThanBalance_throwsException() {
            Money a = Money.of(5_000);
            Money b = Money.of(10_000);

            assertThatThrownBy(() -> a.subtract(b))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("차감 후 금액이 음수가 될 수 없습니다. (잔액 부족)");
        }
    }

    @Nested
    @DisplayName("곱셈 (수수료 계산 등)")
    class Multiply {

        @Test
        @DisplayName("요율을 곱하면 소수점 이하는 버림 처리된다")
        void multiplyRoundsDown() {
            Money amount = Money.of(15_000);

            Money fee = amount.multiply(BigDecimal.valueOf(0.05)); // 15000 * 0.05 = 750

            assertThat(fee.getValue()).isEqualByComparingTo(BigDecimal.valueOf(750));
        }

        @Test
        @DisplayName("소수점 결과가 버려지는 경우를 확인한다")
        void multiplyTruncatesFraction() {
            Money amount = Money.of(999);

            Money fee = amount.multiply(BigDecimal.valueOf(0.1)); // 99.9 -> 99

            assertThat(fee.getValue()).isEqualByComparingTo(BigDecimal.valueOf(99));
        }
    }

    @Nested
    @DisplayName("비교")
    class Compare {

        @Test
        @DisplayName("금액이 크거나 같으면 true를 반환한다")
        void isGreaterThanOrEqual_true() {
            assertThat(Money.of(10_000).isGreaterThanOrEqual(Money.of(10_000))).isTrue();
            assertThat(Money.of(10_000).isGreaterThanOrEqual(Money.of(9_999))).isTrue();
        }

        @Test
        @DisplayName("금액이 작으면 false를 반환한다")
        void isGreaterThanOrEqual_false() {
            assertThat(Money.of(9_999).isGreaterThanOrEqual(Money.of(10_000))).isFalse();
        }

        @Test
        @DisplayName("금액이 작으면 isLessThan은 true를 반환한다")
        void isLessThan_true() {
            assertThat(Money.of(9_999).isLessThan(Money.of(10_000))).isTrue();
        }

        @Test
        @DisplayName("금액이 크거나 같으면 isLessThan은 false를 반환한다")
        void isLessThan_false() {
            assertThat(Money.of(10_000).isLessThan(Money.of(10_000))).isFalse();
        }
    }

    @Nested
    @DisplayName("동등성")
    class Equality {

        @Test
        @DisplayName("값이 같으면 equals는 true를 반환한다")
        void sameValueIsEqual() {
            assertThat(Money.of(10_000)).isEqualTo(Money.of(10_000));
        }

        @Test
        @DisplayName("스케일이 달라도 값이 같으면 equals는 true를 반환한다 (100 == 100.00)")
        void differentScaleSameValueIsEqual() {
            Money a = Money.of(new BigDecimal("100"));
            Money b = Money.of(new BigDecimal("100.00"));

            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }

        @Test
        @DisplayName("값이 다르면 equals는 false를 반환한다")
        void differentValueIsNotEqual() {
            assertThat(Money.of(10_000)).isNotEqualTo(Money.of(9_999));
        }
    }
}