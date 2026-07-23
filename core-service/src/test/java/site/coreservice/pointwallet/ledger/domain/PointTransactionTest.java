package site.coreservice.pointwallet.ledger.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import site.coreservice.pointwallet.shared.Money;

@DisplayName("PointTransaction 엔티티")
class PointTransactionTest {

    private static final Long WALLET_ID = 1L;
    private static final Long RELATED_ID = 100L;

    @Nested
    @DisplayName("기록 (record)")
    class Record {

        @Test
        @DisplayName("충전 사실을 기록하면 전달한 값 그대로 필드에 채워진다")
        void record_전달값_그대로_기록된다() {
            // given
            Money amount = Money.of(10_000);
            Money balanceAfter = Money.of(10_000);

            // when
            PointTransaction transaction = PointTransaction.record(
                    WALLET_ID, PointTransactionType.DEPOSIT, amount, balanceAfter, RELATED_ID
            );

            // then
            assertThat(transaction.getWalletId()).isEqualTo(WALLET_ID);
            assertThat(transaction.getType()).isEqualTo(PointTransactionType.DEPOSIT);
            assertThat(transaction.getAmount()).isEqualTo(amount);
            assertThat(transaction.getBalanceAfter()).isEqualTo(balanceAfter);
            assertThat(transaction.getRelatedId()).isEqualTo(RELATED_ID);
            assertThat(transaction.getOccurredAt()).isNotNull();
        }

        @Test
        @DisplayName("두 번째 충전은 balanceAfter가 누적된 잔액을 반영한다")
        void record_balanceAfter는_누적잔액을_반영한다() {
            // given: 첫 충전 후 잔액 10,000원, 두 번째 충전 5,000원
            Money secondAmount = Money.of(5_000);
            Money accumulatedBalance = Money.of(15_000);

            // when
            PointTransaction transaction = PointTransaction.record(
                    WALLET_ID, PointTransactionType.DEPOSIT, secondAmount, accumulatedBalance, RELATED_ID
            );

            // then: amount는 이번 건의 증가분, balanceAfter는 누적값 — 둘이 다른 값임을 확인
            assertThat(transaction.getAmount()).isEqualTo(secondAmount);
            assertThat(transaction.getBalanceAfter()).isEqualTo(accumulatedBalance);
            assertThat(transaction.getAmount()).isNotEqualTo(transaction.getBalanceAfter());
        }
    }
}