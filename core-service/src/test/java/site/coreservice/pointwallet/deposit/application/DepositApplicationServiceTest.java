package site.coreservice.pointwallet.deposit.application;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import site.coreservice.pointwallet.deposit.domain.Deposit;
import site.coreservice.pointwallet.deposit.domain.DepositRepository;
import site.coreservice.pointwallet.deposit.domain.DepositStatus;
import site.coreservice.pointwallet.deposit.domain.TossCancelResult;
import site.coreservice.pointwallet.deposit.domain.TossConfirmResult;
import site.coreservice.pointwallet.deposit.domain.TossPaymentsClient;
import site.coreservice.pointwallet.deposit.exception.DepositErrorCode;
import site.coreservice.pointwallet.deposit.exception.DepositException;
import site.coreservice.pointwallet.ledger.application.PointTransactionService;
import site.coreservice.pointwallet.ledger.domain.PointTransactionType;
import site.coreservice.pointwallet.shared.Money;
import site.coreservice.pointwallet.wallet.application.WalletBalanceResult;
import site.coreservice.pointwallet.wallet.application.WalletService;
import site.coreservice.pointwallet.wallet.domain.InsufficientBalanceException;
import site.coreservice.pointwallet.wallet.exception.WalletNotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayName("DepositApplicationService")
class DepositApplicationServiceTest {

    @Mock
    private DepositRepository depositRepository;

    @Mock
    private WalletService walletService;

    @Mock
    private PointTransactionService pointTransactionService;

    @Mock
    private TossPaymentsClient tossPaymentsClient;

    private DepositApplicationService sut;

    private static final Long USER_ID = 1L;
    private static final Long WALLET_ID = 100L;
    private static final String ORDER_ID = "DEPOSIT-ORDER-1";
    private static final String PAYMENT_KEY = "toss-payment-key-1";
    private static final Money AMOUNT = Money.of(10_000);

    @BeforeEach
    void setUp() {
        sut = new DepositApplicationService(depositRepository, walletService, pointTransactionService, tossPaymentsClient);
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
        @DisplayName("정상 흐름이면 승인 확정 + 지갑 충전(WalletService 위임) + 원장 기록(PointTransactionService 위임)까지 이어진다")
        void confirmDeposit_정상흐름이면_지갑잔액에_반영된다() {
            // given
            Deposit deposit = Deposit.request(USER_ID, ORDER_ID, AMOUNT);
            when(depositRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(deposit));

            TossConfirmResult tossResult = new TossConfirmResult(PAYMENT_KEY, ORDER_ID, AMOUNT);
            when(tossPaymentsClient.confirmPayment(PAYMENT_KEY, ORDER_ID, AMOUNT)).thenReturn(tossResult);

            when(walletService.charge(USER_ID, AMOUNT)).thenReturn(new WalletBalanceResult(WALLET_ID, AMOUNT));

            // when
            sut.confirmDeposit(PAYMENT_KEY, ORDER_ID, AMOUNT);

            // then: Deposit이 확정됨
            assertThat(deposit.getStatus()).isEqualTo(DepositStatus.DONE);
            assertThat(deposit.getPaymentKey()).isEqualTo(PAYMENT_KEY);

            // then: WalletService에 충전을 위임함
            verify(walletService).charge(USER_ID, AMOUNT);

            // then: PointTransactionService에 원장 기록을 위임함 (WalletService가 돌려준 결과 기준)
            verify(pointTransactionService).record(WALLET_ID, PointTransactionType.DEPOSIT, AMOUNT, AMOUNT, deposit.getId());
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
        @DisplayName("콜백 금액이 요청 금액과 다르면 Deposit을 실패 처리하고 토스 API·WalletService·PointTransactionService를 호출하지 않는다")
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
            verify(walletService, never()).charge(anyLong(), any());
            verify(pointTransactionService, never()).record(any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("충전 취소 (cancelDeposit)")
    class CancelDeposit {

        private final Long DEPOSIT_ID = 999L;

        private Deposit createDoneDeposit() {
            Deposit deposit = Deposit.request(USER_ID, ORDER_ID, AMOUNT);
            deposit.confirm(PAYMENT_KEY, ORDER_ID, AMOUNT);
            ReflectionTestUtils.setField(deposit, "id", DEPOSIT_ID);
            return deposit;
        }

        @Test
        @DisplayName("정상 흐름이면 WalletService.deduct 위임 + Deposit CANCELED 전환 + 토스 취소호출 + 원장기록(PointTransactionService 위임)까지 이어진다")
        void cancelDeposit_정상흐름이면_지갑서비스에_차감을_위임한다() {
            // given
            Deposit deposit = createDoneDeposit();
            when(depositRepository.findById(DEPOSIT_ID)).thenReturn(Optional.of(deposit));

            when(walletService.deduct(USER_ID, AMOUNT)).thenReturn(new WalletBalanceResult(WALLET_ID, Money.zero()));

            when(tossPaymentsClient.cancelPayment(PAYMENT_KEY, "단순 변심", AMOUNT))
                    .thenReturn(new TossCancelResult(PAYMENT_KEY, "cancel-tx-1", AMOUNT));

            // when
            sut.cancelDeposit(DEPOSIT_ID, "단순 변심");

            // then: Deposit이 취소됨
            assertThat(deposit.getStatus()).isEqualTo(DepositStatus.CANCELED);
            assertThat(deposit.getCancelReason()).isEqualTo("단순 변심");

            // then: WalletService에 차감을 위임함
            verify(walletService).deduct(USER_ID, AMOUNT);

            // then: 토스 취소 API가 호출됨
            verify(tossPaymentsClient).cancelPayment(PAYMENT_KEY, "단순 변심", AMOUNT);

            // then: PointTransactionService에 원장 기록을 위임함 (WalletService가 돌려준 결과 기준)
            verify(pointTransactionService).record(
                    WALLET_ID, PointTransactionType.DEPOSIT_CANCEL, AMOUNT, Money.zero(), DEPOSIT_ID
            );
        }

        @Test
        @DisplayName("존재하지 않는 depositId면 예외가 발생하고 토스 API·WalletService·PointTransactionService를 호출하지 않는다")
        void cancelDeposit_존재하지_않으면_예외() {
            // given
            when(depositRepository.findById(DEPOSIT_ID)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sut.cancelDeposit(DEPOSIT_ID, "사유"))
                    .isInstanceOf(DepositException.class)
                    .extracting(e -> ((DepositException) e).getErrorCode())
                    .isEqualTo(DepositErrorCode.DEPOSIT_NOT_FOUND);

            verify(tossPaymentsClient, never()).cancelPayment(any(), any(), any());
            verify(walletService, never()).deduct(anyLong(), any());
            verify(pointTransactionService, never()).record(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("DONE 상태가 아니면 예외가 발생하고 토스 API·WalletService·PointTransactionService를 호출하지 않는다")
        void cancelDeposit_DONE상태가_아니면_예외() {
            // given
            Deposit deposit = Deposit.request(USER_ID, ORDER_ID, AMOUNT); // REQUESTED
            ReflectionTestUtils.setField(deposit, "id", DEPOSIT_ID);
            when(depositRepository.findById(DEPOSIT_ID)).thenReturn(Optional.of(deposit));

            // when & then
            assertThatThrownBy(() -> sut.cancelDeposit(DEPOSIT_ID, "사유"))
                    .isInstanceOf(DepositException.class)
                    .extracting(e -> ((DepositException) e).getErrorCode())
                    .isEqualTo(DepositErrorCode.ALREADY_PROCESSED_DEPOSIT);

            verify(tossPaymentsClient, never()).cancelPayment(any(), any(), any());
            verify(walletService, never()).deduct(anyLong(), any());
            verify(pointTransactionService, never()).record(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("지갑이 없으면 WALLET_NOT_FOUND로 번역되고 토스 API·PointTransactionService를 호출하지 않는다")
        void cancelDeposit_지갑이_없으면_WALLET_NOT_FOUND로_번역() {
            // given
            Deposit deposit = createDoneDeposit();
            when(depositRepository.findById(DEPOSIT_ID)).thenReturn(Optional.of(deposit));
            when(walletService.deduct(USER_ID, AMOUNT)).thenThrow(new WalletNotFoundException());

            // when & then
            assertThatThrownBy(() -> sut.cancelDeposit(DEPOSIT_ID, "사유"))
                    .isInstanceOf(DepositException.class)
                    .extracting(e -> ((DepositException) e).getErrorCode())
                    .isEqualTo(DepositErrorCode.WALLET_NOT_FOUND);

            verify(tossPaymentsClient, never()).cancelPayment(any(), any(), any());
            verify(pointTransactionService, never()).record(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("이미 써버려서 지갑 잔액이 부족하면 CANCEL_INSUFFICIENT_BALANCE로 번역되고 토스 API·PointTransactionService를 호출하지 않는다")
        void cancelDeposit_잔액부족이면_토스호출안하고_예외() {
            // given
            Deposit deposit = createDoneDeposit();
            when(depositRepository.findById(DEPOSIT_ID)).thenReturn(Optional.of(deposit));
            when(walletService.deduct(USER_ID, AMOUNT)).thenThrow(new InsufficientBalanceException());

            // when & then
            assertThatThrownBy(() -> sut.cancelDeposit(DEPOSIT_ID, "사유"))
                    .isInstanceOf(DepositException.class)
                    .extracting(e -> ((DepositException) e).getErrorCode())
                    .isEqualTo(DepositErrorCode.CANCEL_INSUFFICIENT_BALANCE);

            assertThat(deposit.getStatus()).isEqualTo(DepositStatus.CANCELED);
            verify(tossPaymentsClient, never()).cancelPayment(any(), any(), any());
            verify(pointTransactionService, never()).record(any(), any(), any(), any(), any());
        }
    }
}