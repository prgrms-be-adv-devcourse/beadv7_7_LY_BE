package site.pointwalletservice.wallet.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import site.pointwalletservice.shared.Money;

@DisplayName("Wallet 엔티티")
class WalletTest {

    private static final Long USER_ID = 1L;

    @Nested
    @DisplayName("개설 (open)")
    class Open {

        @Test
        @DisplayName("지갑을 개설하면 잔액 0으로 생성된다")
        void open_잔액_0으로_생성된다() {
            // given & when
            Wallet wallet = Wallet.open(USER_ID);

            // then
            assertThat(wallet.getUserId()).isEqualTo(USER_ID);
            assertThat(wallet.getBalance()).isEqualTo(Money.zero());
        }
    }

    @Nested
    @DisplayName("충전 (charge)")
    class Charge {

        @Test
        @DisplayName("충전하면 잔액이 그만큼 늘어난다")
        void charge_잔액이_늘어난다() {
            // given
            Wallet wallet = Wallet.open(USER_ID);

            // when
            wallet.charge(Money.of(10_000));

            // then
            assertThat(wallet.getBalance()).isEqualTo(Money.of(10_000));
        }

        @Test
        @DisplayName("여러 번 충전하면 누적된다")
        void charge_여러번_누적된다() {
            // given
            Wallet wallet = Wallet.open(USER_ID);

            // when
            wallet.charge(Money.of(10_000));
            wallet.charge(Money.of(5_000));

            // then
            assertThat(wallet.getBalance()).isEqualTo(Money.of(15_000));
        }

        @Test
        @DisplayName("잔액이 부족하면 스스로 InsufficientBalanceException을 던지고 잔액은 변하지 않는다")
        void deduct_잔액부족하면_예외를_던지고_잔액은_그대로다() {
            Wallet wallet = Wallet.open(USER_ID);
            wallet.charge(Money.of(5_000));

            assertThatThrownBy(() -> wallet.deduct(Money.of(10_000)))
                    .isInstanceOf(InsufficientBalanceException.class);

            assertThat(wallet.getBalance()).isEqualTo(Money.of(5_000));
        }

        @Test
        @DisplayName("잔액과 정확히 같은 금액은 차감할 수 있다 (경계값)")
        void deduct_잔액과_정확히_같으면_0으로_차감된다() {
            Wallet wallet = Wallet.open(USER_ID);
            wallet.charge(Money.of(10_000));

            wallet.deduct(Money.of(10_000));

            assertThat(wallet.getBalance()).isEqualTo(Money.zero());
        }
    }
}