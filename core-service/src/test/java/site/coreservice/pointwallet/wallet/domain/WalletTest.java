package site.coreservice.pointwallet.wallet.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import site.coreservice.pointwallet.shared.Money;

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
    }
}