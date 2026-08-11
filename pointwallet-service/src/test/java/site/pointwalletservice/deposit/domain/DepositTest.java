package site.pointwalletservice.deposit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import site.pointwalletservice.deposit.exception.DepositErrorCode;
import site.pointwalletservice.deposit.exception.DepositException;
import site.pointwalletservice.shared.Money;


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
            assertThat(deposit.getProviderTransactionId()).isNull();
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
            String providerTxId = "toss-payment-key-1";

            // when
            deposit.confirm(providerTxId, ORDER_ID, REQUESTED_AMOUNT);

            // then
            assertThat(deposit.getStatus()).isEqualTo(DepositStatus.DONE);
            assertThat(deposit.getProviderTransactionId()).isEqualTo(providerTxId);
            assertThat(deposit.getApprovedAt()).isNotNull();
        }

        @Test
        @DisplayName("이미 DONE인 건을 다시 confirm하면 예외가 발생한다")
        void confirm_이미_처리된_건이면_예외() {
            // given
            Deposit deposit = createRequestedDeposit();
            deposit.confirm("first-provider-tx-id", ORDER_ID, REQUESTED_AMOUNT);

            // when & then
            assertThatThrownBy(() -> deposit.confirm("second-provider-tx-id", ORDER_ID, REQUESTED_AMOUNT))
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
            assertThatThrownBy(() -> deposit.confirm("provider-tx-id", ORDER_ID, REQUESTED_AMOUNT))
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
            assertThatThrownBy(() -> deposit.confirm("provider-tx-id", "다른-주문번호", REQUESTED_AMOUNT))
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
            assertThatThrownBy(() -> deposit.confirm("provider-tx-id", ORDER_ID, differentAmount))
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
            deposit.confirm("provider-tx-id", ORDER_ID, REQUESTED_AMOUNT);

            // when & then
            assertThatThrownBy(deposit::fail)
                    .isInstanceOf(DepositException.class)
                    .extracting(e -> ((DepositException) e).getErrorCode())
                    .isEqualTo(DepositErrorCode.ALREADY_PROCESSED_DEPOSIT);
        }
    }

    @Nested
    @DisplayName("질의 메서드 (isConfirmable / isCancelable / matchesAmount)")
    class QueryMethods {

        @Test
        @DisplayName("REQUESTED 상태면 isConfirmable은 true, isCancelable은 false다")
        void REQUESTED_상태면_isConfirmable_true_isCancelable_false() {
            // given
            Deposit deposit = createRequestedDeposit();

            // when & then
            assertThat(deposit.isConfirmable()).isTrue();
            assertThat(deposit.isCancelable()).isFalse();
        }

        @Test
        @DisplayName("DONE 상태면 isConfirmable은 false, isCancelable은 true다")
        void DONE_상태면_isConfirmable_false_isCancelable_true() {
            // given
            Deposit deposit = createRequestedDeposit();
            deposit.confirm("provider-tx-id", ORDER_ID, REQUESTED_AMOUNT);

            // when & then
            assertThat(deposit.isConfirmable()).isFalse();
            assertThat(deposit.isCancelable()).isTrue();
        }

        @Test
        @DisplayName("FAILED/CANCELED 상태면 isConfirmable·isCancelable 모두 false다")
        void FAILED_상태면_둘다_false() {
            // given
            Deposit deposit = createRequestedDeposit();
            deposit.fail();

            // when & then
            assertThat(deposit.isConfirmable()).isFalse();
            assertThat(deposit.isCancelable()).isFalse();
        }

        @Test
        @DisplayName("matchesAmount는 요청 금액과 같을 때만 true를 반환한다")
        void matchesAmount_요청금액과_같을때만_true() {
            // given
            Deposit deposit = createRequestedDeposit();

            // when & then
            assertThat(deposit.matchesAmount(REQUESTED_AMOUNT)).isTrue();
            assertThat(deposit.matchesAmount(Money.of(1))).isFalse();
        }
    }
}