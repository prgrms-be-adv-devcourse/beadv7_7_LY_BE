package site.coreservice.pointwallet.deposit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import site.coreservice.pointwallet.deposit.exception.DepositErrorCode;
import site.coreservice.pointwallet.deposit.exception.DepositException;
import site.coreservice.pointwallet.shared.Money;

@DisplayName("Deposit 엔티티")
class DepositTest {

    private static final Long USER_ID = 1L;
    private static final String ORDER_ID = "DEPOSIT-ORDER-1";
    private static final Money REQUESTED_AMOUNT = Money.of(10_000);

    private Deposit createRequestedDeposit() {
        return Deposit.request(USER_ID, ORDER_ID, REQUESTED_AMOUNT);
    }

    @Nested
    @DisplayName("생성")
    class Request {

        @Test
        @DisplayName("충전을 요청하면 REQUESTED 상태로 생성된다")
        void request_REQUESTED_상태로_생성된다() {
            // given & when
            Deposit deposit = Deposit.request(USER_ID, ORDER_ID, REQUESTED_AMOUNT);

            // then
            assertThat(deposit.getUserId()).isEqualTo(USER_ID);
            assertThat(deposit.getOrderId()).isEqualTo(ORDER_ID);
            assertThat(deposit.getRequestedAmount()).isEqualTo(REQUESTED_AMOUNT);
            assertThat(deposit.getStatus()).isEqualTo(DepositStatus.REQUESTED);
            assertThat(deposit.getRequestedAt()).isNotNull();
            assertThat(deposit.getPaymentKey()).isNull();
            assertThat(deposit.getApprovedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("승인 확정 (confirm)")
    class Confirm {

        @Test
        @DisplayName("orderId·금액이 일치하면 DONE 상태로 확정된다")
        void confirm_일치하면_DONE으로_확정된다() {
            // given
            Deposit deposit = createRequestedDeposit();
            String paymentKey = "toss-payment-key-1";

            // when
            deposit.confirm(paymentKey, ORDER_ID, REQUESTED_AMOUNT);

            // then
            assertThat(deposit.getStatus()).isEqualTo(DepositStatus.DONE);
            assertThat(deposit.getPaymentKey()).isEqualTo(paymentKey);
            assertThat(deposit.getApprovedAt()).isNotNull();
        }

        @Test
        @DisplayName("이미 DONE인 건을 다시 confirm하면 예외가 발생한다")
        void confirm_이미_처리된_건이면_예외() {
            // given
            Deposit deposit = createRequestedDeposit();
            deposit.confirm("first-payment-key", ORDER_ID, REQUESTED_AMOUNT);

            // when & then
            assertThatThrownBy(() -> deposit.confirm("second-payment-key", ORDER_ID, REQUESTED_AMOUNT))
                    .isInstanceOf(DepositException.class)
                    .extracting(e -> ((DepositException) e).getErrorCode())
                    .isEqualTo(DepositErrorCode.ALREADY_PROCESSED_DEPOSIT);
        }

        @Test
        @DisplayName("이미 FAILED인 건을 confirm하면 예외가 발생한다")
        void confirm_이미_실패처리된_건이면_예외() {
            // given
            Deposit deposit = createRequestedDeposit();
            deposit.fail();

            // when & then
            assertThatThrownBy(() -> deposit.confirm("payment-key", ORDER_ID, REQUESTED_AMOUNT))
                    .isInstanceOf(DepositException.class)
                    .extracting(e -> ((DepositException) e).getErrorCode())
                    .isEqualTo(DepositErrorCode.ALREADY_PROCESSED_DEPOSIT);
        }

        @Test
        @DisplayName("요청 시점과 다른 orderId로 confirm하면 예외가 발생한다")
        void confirm_orderId가_다르면_예외() {
            // given
            Deposit deposit = createRequestedDeposit();

            // when & then
            assertThatThrownBy(() -> deposit.confirm("payment-key", "다른-주문번호", REQUESTED_AMOUNT))
                    .isInstanceOf(DepositException.class)
                    .extracting(e -> ((DepositException) e).getErrorCode())
                    .isEqualTo(DepositErrorCode.ORDER_ID_MISMATCH);
        }

        @Test
        @DisplayName("요청 금액과 다른 금액으로 confirm하면 예외가 발생한다")
        void confirm_금액이_다르면_예외() {
            // given
            Deposit deposit = createRequestedDeposit();
            Money differentAmount = Money.of(5_000);

            // when & then
            assertThatThrownBy(() -> deposit.confirm("payment-key", ORDER_ID, differentAmount))
                    .isInstanceOf(DepositException.class)
                    .extracting(e -> ((DepositException) e).getErrorCode())
                    .isEqualTo(DepositErrorCode.AMOUNT_MISMATCH);
        }
    }

    @Nested
    @DisplayName("실패 처리 (fail)")
    class Fail {

        @Test
        @DisplayName("REQUESTED 상태에서 fail하면 FAILED 상태가 된다")
        void fail_REQUESTED_상태면_FAILED로_바뀐다() {
            // given
            Deposit deposit = createRequestedDeposit();

            // when
            deposit.fail();

            // then
            assertThat(deposit.getStatus()).isEqualTo(DepositStatus.FAILED);
        }

        @Test
        @DisplayName("이미 DONE인 건을 fail하면 예외가 발생한다")
        void fail_이미_DONE인_건이면_예외() {
            // given
            Deposit deposit = createRequestedDeposit();
            deposit.confirm("payment-key", ORDER_ID, REQUESTED_AMOUNT);

            // when & then
            assertThatThrownBy(deposit::fail)
                    .isInstanceOf(DepositException.class)
                    .extracting(e -> ((DepositException) e).getErrorCode())
                    .isEqualTo(DepositErrorCode.ALREADY_PROCESSED_DEPOSIT);
        }
    }
}