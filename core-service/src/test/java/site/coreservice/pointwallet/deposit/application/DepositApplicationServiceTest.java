package site.coreservice.pointwallet.deposit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import site.coreservice.pointwallet.deposit.domain.Deposit;
import site.coreservice.pointwallet.deposit.exception.DepositErrorCode;
import site.coreservice.pointwallet.deposit.exception.DepositException;
import site.coreservice.pointwallet.deposit.domain.DepositRepository;
import site.coreservice.pointwallet.deposit.domain.DepositStatus;
import site.coreservice.pointwallet.deposit.domain.TossConfirmResult;
import site.coreservice.pointwallet.deposit.domain.TossPaymentsClient;
import site.coreservice.pointwallet.ledger.domain.PointTransaction;
import site.coreservice.pointwallet.ledger.domain.PointTransactionRepository;
import site.coreservice.pointwallet.shared.Money;
import site.coreservice.pointwallet.wallet.domain.Wallet;
import site.coreservice.pointwallet.wallet.domain.WalletRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("DepositApplicationService")
class DepositApplicationServiceTest {

    @Mock
    private DepositRepository depositRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private PointTransactionRepository pointTransactionRepository;

    @Mock
    private TossPaymentsClient tossPaymentsClient;

    private DepositApplicationService sut;

    private static final Long USER_ID = 1L;
    private static final String ORDER_ID = "DEPOSIT-ORDER-1";
    private static final String PAYMENT_KEY = "toss-payment-key-1";
    private static final Money AMOUNT = Money.of(10_000);

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        sut = new DepositApplicationService(depositRepository, walletRepository, pointTransactionRepository, tossPaymentsClient);
    }

    @Nested
    @DisplayName("충전 요청 (requestDeposit)")
    class RequestDeposit {

        @Test
        @DisplayName("REQUESTED 상태의 Deposit을 저장하고 주문 정보를 반환한다")
        void requestDeposit_저장하고_주문정보를_반환한다() {
            // given
            when(depositRepository.save(any(Deposit.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            DepositRequestResult result = sut.requestDeposit(USER_ID, AMOUNT);

            // then
            ArgumentCaptor<Deposit> captor = ArgumentCaptor.forClass(Deposit.class);
            verify(depositRepository).save(captor.capture());

            Deposit saved = captor.getValue();
            assertThat(saved.getUserId()).isEqualTo(USER_ID);
            assertThat(saved.getStatus()).isEqualTo(DepositStatus.REQUESTED);
            assertThat(saved.getRequestedAmount()).isEqualTo(AMOUNT);

            assertThat(result.orderId()).isEqualTo(saved.getOrderId());
            assertThat(result.amount()).isEqualTo(AMOUNT);
        }
    }

    @Nested
    @DisplayName("충전 확정 (confirmDeposit)")
    class ConfirmDeposit {

        @Test
        @DisplayName("정상 흐름이면 승인 확정 + 지갑 잔액 반영 + 원장 기록까지 이어진다")
        void confirmDeposit_정상흐름이면_지갑잔액에_반영된다() {
            // given
            Deposit deposit = Deposit.request(USER_ID, ORDER_ID, AMOUNT);
            when(depositRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(deposit));

            TossConfirmResult tossResult = new TossConfirmResult(PAYMENT_KEY, ORDER_ID, AMOUNT);
            when(tossPaymentsClient.confirmPayment(PAYMENT_KEY, ORDER_ID, AMOUNT)).thenReturn(tossResult);

            Wallet existingWallet = Wallet.open(USER_ID);
            ReflectionTestUtils.setField(existingWallet, "id", 100L);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existingWallet));
            when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            sut.confirmDeposit(PAYMENT_KEY, ORDER_ID, AMOUNT);

            // then: Deposit이 확정됨
            assertThat(deposit.getStatus()).isEqualTo(DepositStatus.DONE);
            assertThat(deposit.getPaymentKey()).isEqualTo(PAYMENT_KEY);

            // then: 지갑 잔액이 늘어남
            assertThat(existingWallet.getBalance()).isEqualTo(AMOUNT);
            verify(walletRepository).save(existingWallet);

            // then: 원장에 사실 1건이 기록됨
            ArgumentCaptor<PointTransaction> txCaptor = ArgumentCaptor.forClass(PointTransaction.class);
            verify(pointTransactionRepository).save(txCaptor.capture());

            PointTransaction transaction = txCaptor.getValue();
            assertThat(transaction.getWalletId()).isEqualTo(100L);
            assertThat(transaction.getAmount()).isEqualTo(AMOUNT);
            assertThat(transaction.getBalanceAfter()).isEqualTo(AMOUNT);
            assertThat(transaction.getRelatedId()).isEqualTo(deposit.getId());
        }

        @Test
        @DisplayName("지갑이 없던 사용자면 새 지갑을 개설하고 반영한다")
        void confirmDeposit_지갑이_없으면_새로_개설한다() {
            // given
            Deposit deposit = Deposit.request(USER_ID, ORDER_ID, AMOUNT);
            when(depositRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(deposit));
            when(tossPaymentsClient.confirmPayment(PAYMENT_KEY, ORDER_ID, AMOUNT))
                    .thenReturn(new TossConfirmResult(PAYMENT_KEY, ORDER_ID, AMOUNT));

            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            sut.confirmDeposit(PAYMENT_KEY, ORDER_ID, AMOUNT);

            // then: 지갑 저장이 두 번 일어남 (개설 시 1번 + 충전 반영 후 1번)
            verify(walletRepository, times(2)).save(any(Wallet.class));
            verify(pointTransactionRepository).save(any(PointTransaction.class));
        }

        @Test
        @DisplayName("존재하지 않는 orderId면 예외가 발생하고 토스 API를 호출하지 않는다")
        void confirmDeposit_주문을_못찾으면_예외() {
            // given
            when(depositRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sut.confirmDeposit(PAYMENT_KEY, ORDER_ID, AMOUNT))
                    .isInstanceOf(DepositException.class)
                    .extracting(e -> ((DepositException) e).getErrorCode())
                    .isEqualTo(DepositErrorCode.DEPOSIT_NOT_FOUND);

            verify(tossPaymentsClient, never()).confirmPayment(any(), any(), any());
        }

        @Test
        @DisplayName("콜백 금액이 요청 금액과 다르면 Deposit을 실패 처리하고 토스 API를 호출하지 않는다")
        void confirmDeposit_콜백금액불일치면_실패처리하고_토스API_호출안함() {
            // given
            Deposit deposit = Deposit.request(USER_ID, ORDER_ID, AMOUNT);
            when(depositRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(deposit));
            Money tamperedAmount = Money.of(1_000);

            // when & then
            assertThatThrownBy(() -> sut.confirmDeposit(PAYMENT_KEY, ORDER_ID, tamperedAmount))
                    .isInstanceOf(DepositException.class)
                    .extracting(e -> ((DepositException) e).getErrorCode())
                    .isEqualTo(DepositErrorCode.AMOUNT_MISMATCH);

            assertThat(deposit.getStatus()).isEqualTo(DepositStatus.FAILED);
            verify(tossPaymentsClient, never()).confirmPayment(any(), any(), any());
            verify(walletRepository, never()).findByUserId(anyLong());
        }
    }
}