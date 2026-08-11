package site.pointwalletservice.deposit.application;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import site.pointwalletservice.deposit.domain.Deposit;
import site.pointwalletservice.deposit.domain.DepositRepository;
import site.pointwalletservice.deposit.domain.DepositStatus;
import site.pointwalletservice.deposit.domain.PaymentGatewayClient;
import site.pointwalletservice.deposit.domain.PgApproveResult;
import site.pointwalletservice.deposit.domain.PgCancelResult;
import site.pointwalletservice.deposit.domain.PgInquiryResult;
import site.pointwalletservice.deposit.exception.DepositErrorCode;
import site.pointwalletservice.deposit.exception.DepositException;
import site.pointwalletservice.ledger.application.PointTransactionService;
import site.pointwalletservice.ledger.domain.PointTransactionType;
import site.pointwalletservice.shared.Money;
import site.pointwalletservice.wallet.application.WalletBalanceResult;
import site.pointwalletservice.wallet.application.WalletService;
import site.pointwalletservice.wallet.domain.InsufficientBalanceException;
import site.pointwalletservice.wallet.exception.WalletNotFoundException;


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
    private PaymentGatewayClient paymentGatewayClient;

    @Mock
    private TransactionTemplate transactionTemplate;

    private DepositApplicationService sut;

    private static final Long USER_ID = 1L;
    private static final Long WALLET_ID = 100L;
    private static final String ORDER_ID = "DEPOSIT-ORDER-1";
    private static final String PROVIDER_TX_ID = "toss-payment-key-1";
    private static final Money AMOUNT = Money.of(10_000);

    @BeforeEach
    void setUp() {
        lenient().doAnswer(invocation -> {
            Consumer<TransactionStatus> action = invocation.getArgument(0);
            action.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        lenient().doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        }).when(transactionTemplate).execute(any());

        sut = new DepositApplicationService(
                depositRepository, walletService, pointTransactionService, paymentGatewayClient, transactionTemplate
        );
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
            Deposit deposit = Deposit.request(USER_ID, ORDER_ID, AMOUNT);
            when(depositRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(deposit));

            PgApproveResult approveResult = new PgApproveResult(PROVIDER_TX_ID, ORDER_ID, AMOUNT);
            when(paymentGatewayClient.approve(PROVIDER_TX_ID, ORDER_ID, AMOUNT)).thenReturn(approveResult);

            when(walletService.charge(USER_ID, AMOUNT)).thenReturn(new WalletBalanceResult(WALLET_ID, AMOUNT));

            sut.confirmDeposit(PROVIDER_TX_ID, ORDER_ID, AMOUNT);

            assertThat(deposit.getStatus()).isEqualTo(DepositStatus.DONE);
            assertThat(deposit.getProviderTransactionId()).isEqualTo(PROVIDER_TX_ID);

            verify(walletService).charge(USER_ID, AMOUNT);
            verify(pointTransactionService).record(WALLET_ID, PointTransactionType.DEPOSIT, AMOUNT, AMOUNT, deposit.getId());
            verify(depositRepository).save(deposit);
        }

        @Test
        @DisplayName("존재하지 않는 orderId면 예외가 발생하고 PG API를 호출하지 않는다")
        void confirmDeposit_주문을_못찾으면_예외() {
            when(depositRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sut.confirmDeposit(PROVIDER_TX_ID, ORDER_ID, AMOUNT))
                    .isInstanceOf(DepositException.class)
                    .extracting(e -> ((DepositException) e).getErrorCode())
                    .isEqualTo(DepositErrorCode.DEPOSIT_NOT_FOUND);

            verify(paymentGatewayClient, never()).approve(any(), any(), any());
        }

        @Test
        @DisplayName("콜백 금액이 요청 금액과 다르면 Deposit을 실패 처리하고 PG API·WalletService·PointTransactionService를 호출하지 않는다")
        void confirmDeposit_콜백금액불일치면_실패처리하고_PG_API_호출안함() {
            Deposit deposit = Deposit.request(USER_ID, ORDER_ID, AMOUNT);
            when(depositRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(deposit));
            Money tamperedAmount = Money.of(1_000);

            assertThatThrownBy(() -> sut.confirmDeposit(PROVIDER_TX_ID, ORDER_ID, tamperedAmount))
                    .isInstanceOf(DepositException.class)
                    .extracting(e -> ((DepositException) e).getErrorCode())
                    .isEqualTo(DepositErrorCode.AMOUNT_MISMATCH);

            assertThat(deposit.getStatus()).isEqualTo(DepositStatus.FAILED);
            verify(depositRepository).save(deposit);
            verify(paymentGatewayClient, never()).approve(any(), any(), any());
            verify(walletService, never()).charge(anyLong(), any());
            verify(pointTransactionService, never()).record(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("PG 승인은 성공했지만 DB 반영이 실패하면 보정 취소를 호출하고 예외를 그대로 던진다")
        void confirmDeposit_DB반영실패하면_보정취소하고_예외전파() {
            Deposit deposit = Deposit.request(USER_ID, ORDER_ID, AMOUNT);
            when(depositRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(deposit));

            PgApproveResult approveResult = new PgApproveResult(PROVIDER_TX_ID, ORDER_ID, AMOUNT);
            when(paymentGatewayClient.approve(PROVIDER_TX_ID, ORDER_ID, AMOUNT)).thenReturn(approveResult);
            when(walletService.charge(USER_ID, AMOUNT)).thenReturn(new WalletBalanceResult(WALLET_ID, AMOUNT));

            doAnswerThrowOnExecuteWithoutResult();

            assertThatThrownBy(() -> sut.confirmDeposit(PROVIDER_TX_ID, ORDER_ID, AMOUNT))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("DB 저장 실패");

            verify(paymentGatewayClient).cancel(PROVIDER_TX_ID, "내부 저장 실패로 인한 자동 취소", AMOUNT);
        }

        @Test
        @DisplayName("PG 승인 성공 후 DB 반영도, 보정 취소도 실패하면 PG 조회 API로 실제 상태를 확인한다")
        void confirmDeposit_보정취소마저_실패하면_PG조회를_호출한다() {
            Deposit deposit = Deposit.request(USER_ID, ORDER_ID, AMOUNT);
            when(depositRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(deposit));

            PgApproveResult approveResult = new PgApproveResult(PROVIDER_TX_ID, ORDER_ID, AMOUNT);
            when(paymentGatewayClient.approve(PROVIDER_TX_ID, ORDER_ID, AMOUNT)).thenReturn(approveResult);
            when(walletService.charge(USER_ID, AMOUNT)).thenReturn(new WalletBalanceResult(WALLET_ID, AMOUNT));
            doAnswerThrowOnExecuteWithoutResult();

            when(paymentGatewayClient.cancel(PROVIDER_TX_ID, "내부 저장 실패로 인한 자동 취소", AMOUNT))
                    .thenThrow(new RuntimeException("보정 취소마저 실패(시뮬레이션)"));
            when(paymentGatewayClient.inquire(PROVIDER_TX_ID))
                    .thenReturn(new PgInquiryResult(PROVIDER_TX_ID, ORDER_ID, AMOUNT, "DONE"));

            assertThatThrownBy(() -> sut.confirmDeposit(PROVIDER_TX_ID, ORDER_ID, AMOUNT))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("DB 저장 실패");

            verify(paymentGatewayClient).inquire(PROVIDER_TX_ID);
        }

        @Test
        @DisplayName("보정 취소도, PG 조회도 모두 실패해도 원래 예외는 그대로 전파된다")
        void confirmDeposit_조회마저_실패해도_원래예외가_전파된다() {
            Deposit deposit = Deposit.request(USER_ID, ORDER_ID, AMOUNT);
            when(depositRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(deposit));

            PgApproveResult approveResult = new PgApproveResult(PROVIDER_TX_ID, ORDER_ID, AMOUNT);
            when(paymentGatewayClient.approve(PROVIDER_TX_ID, ORDER_ID, AMOUNT)).thenReturn(approveResult);
            when(walletService.charge(USER_ID, AMOUNT)).thenReturn(new WalletBalanceResult(WALLET_ID, AMOUNT));
            doAnswerThrowOnExecuteWithoutResult();

            when(paymentGatewayClient.cancel(PROVIDER_TX_ID, "내부 저장 실패로 인한 자동 취소", AMOUNT))
                    .thenThrow(new RuntimeException("보정 취소마저 실패(시뮬레이션)"));
            when(paymentGatewayClient.inquire(PROVIDER_TX_ID))
                    .thenThrow(new RuntimeException("조회마저 실패(시뮬레이션)"));

            assertThatThrownBy(() -> sut.confirmDeposit(PROVIDER_TX_ID, ORDER_ID, AMOUNT))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("DB 저장 실패");
        }

        private void doAnswerThrowOnExecuteWithoutResult() {
            org.mockito.Mockito.doAnswer(invocation -> {
                Consumer<TransactionStatus> action = invocation.getArgument(0);
                action.accept(null);
                throw new RuntimeException("DB 저장 실패(시뮬레이션)");
            }).when(transactionTemplate).executeWithoutResult(any());
        }
    }
    @Nested
    @DisplayName("충전 취소 (cancelDeposit)")
    class CancelDeposit {

        private final Long DEPOSIT_ID = 999L;

        private Deposit createDoneDeposit() {
            Deposit deposit = Deposit.request(USER_ID, ORDER_ID, AMOUNT);
            deposit.confirm(PROVIDER_TX_ID, ORDER_ID, AMOUNT);
            ReflectionTestUtils.setField(deposit, "id", DEPOSIT_ID);
            return deposit;
        }

        @Test
        @DisplayName("정상 흐름이면 WalletService.deduct 위임 + Deposit CANCELED 전환 + PG 취소호출 + 원장기록(PointTransactionService 위임)까지 이어진다")
        void cancelDeposit_정상흐름이면_지갑서비스에_차감을_위임한다() {
            // given
            Deposit deposit = createDoneDeposit();
            when(depositRepository.findById(DEPOSIT_ID)).thenReturn(Optional.of(deposit));

            when(walletService.deduct(USER_ID, AMOUNT)).thenReturn(new WalletBalanceResult(WALLET_ID, Money.zero()));

            when(paymentGatewayClient.cancel(PROVIDER_TX_ID, "단순 변심", AMOUNT))
                    .thenReturn(new PgCancelResult(PROVIDER_TX_ID, "cancel-tx-1", AMOUNT));

            // when
            sut.cancelDeposit(DEPOSIT_ID, "단순 변심");

            // then: Deposit이 취소됨
            assertThat(deposit.getStatus()).isEqualTo(DepositStatus.CANCELED);
            assertThat(deposit.getCancelReason()).isEqualTo("단순 변심");

            // then: WalletService에 차감을 위임함
            verify(walletService).deduct(USER_ID, AMOUNT);

            // then: PG 취소 API가 호출됨
            verify(paymentGatewayClient).cancel(PROVIDER_TX_ID, "단순 변심", AMOUNT);

            // then: PointTransactionService에 원장 기록을 위임함
            verify(pointTransactionService).record(
                    WALLET_ID, PointTransactionType.DEPOSIT_CANCEL, AMOUNT, Money.zero(), DEPOSIT_ID
            );

            // then: DB 반영이 실제로 저장을 호출함
            verify(depositRepository).save(deposit);
        }

        @Test
        @DisplayName("존재하지 않는 depositId면 예외가 발생하고 PG API·WalletService·PointTransactionService를 호출하지 않는다")
        void cancelDeposit_존재하지_않으면_예외() {
            // given
            when(depositRepository.findById(DEPOSIT_ID)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sut.cancelDeposit(DEPOSIT_ID, "사유"))
                    .isInstanceOf(DepositException.class)
                    .extracting(e -> ((DepositException) e).getErrorCode())
                    .isEqualTo(DepositErrorCode.DEPOSIT_NOT_FOUND);

            verify(paymentGatewayClient, never()).cancel(any(), any(), any());
            verify(walletService, never()).deduct(anyLong(), any());
            verify(pointTransactionService, never()).record(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("DONE 상태가 아니면 예외가 발생하고 PG API·WalletService·PointTransactionService를 호출하지 않는다")
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

            verify(paymentGatewayClient, never()).cancel(any(), any(), any());
            verify(walletService, never()).deduct(anyLong(), any());
            verify(pointTransactionService, never()).record(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("지갑이 없으면 WALLET_NOT_FOUND로 번역되고 PG API·PointTransactionService를 호출하지 않는다")
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

            verify(paymentGatewayClient, never()).cancel(any(), any(), any());
            verify(pointTransactionService, never()).record(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("이미 써버려서 지갑 잔액이 부족하면 CANCEL_INSUFFICIENT_BALANCE로 번역되고 PG API·PointTransactionService를 호출하지 않는다")
        void cancelDeposit_잔액부족이면_PG호출안하고_예외() {
            // given
            Deposit deposit = createDoneDeposit();
            when(depositRepository.findById(DEPOSIT_ID)).thenReturn(Optional.of(deposit));
            when(walletService.deduct(USER_ID, AMOUNT)).thenThrow(new InsufficientBalanceException());

            // when & then
            assertThatThrownBy(() -> sut.cancelDeposit(DEPOSIT_ID, "사유"))
                    .isInstanceOf(DepositException.class)
                    .extracting(e -> ((DepositException) e).getErrorCode())
                    .isEqualTo(DepositErrorCode.CANCEL_INSUFFICIENT_BALANCE);

            // 지갑 검증에서 막혔으므로 아직 취소 확정 전 상태(DONE)로 남는다
            assertThat(deposit.getStatus()).isEqualTo(DepositStatus.DONE);
            verify(paymentGatewayClient, never()).cancel(any(), any(), any());
            verify(pointTransactionService, never()).record(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("지갑 차감 후 PG 취소가 실패하면 차감을 보정(재충전)하고 예외를 그대로 던진다")
        void cancelDeposit_PG취소실패하면_차감보정하고_예외전파() {
            // given
            Deposit deposit = createDoneDeposit();
            when(depositRepository.findById(DEPOSIT_ID)).thenReturn(Optional.of(deposit));
            when(walletService.deduct(USER_ID, AMOUNT)).thenReturn(new WalletBalanceResult(WALLET_ID, Money.zero()));
            when(paymentGatewayClient.cancel(PROVIDER_TX_ID, "사유", AMOUNT))
                    .thenThrow(new RuntimeException("PG 취소 실패(시뮬레이션)"));

            // when & then
            assertThatThrownBy(() -> sut.cancelDeposit(DEPOSIT_ID, "사유"))
                    .isInstanceOf(RuntimeException.class);

            // then: 차감 보정(재충전)이 호출됨
            verify(walletService).charge(USER_ID, AMOUNT);
            // then: 최종 DB 반영은 일어나지 않음
            verify(pointTransactionService, never()).record(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("PG 취소 실패 후 차감 보정(재충전)마저 실패하면 PG 조회 API로 실제 상태를 확인한다")
        void cancelDeposit_차감보정마저_실패하면_PG조회를_호출한다() {
            // given
            Deposit deposit = createDoneDeposit();
            when(depositRepository.findById(DEPOSIT_ID)).thenReturn(Optional.of(deposit));
            when(walletService.deduct(USER_ID, AMOUNT)).thenReturn(new WalletBalanceResult(WALLET_ID, Money.zero()));
            when(paymentGatewayClient.cancel(PROVIDER_TX_ID, "사유", AMOUNT))
                    .thenThrow(new RuntimeException("PG 취소 실패(시뮬레이션)"));
            when(walletService.charge(USER_ID, AMOUNT))
                    .thenThrow(new RuntimeException("차감 보정마저 실패(시뮬레이션)"));
            when(paymentGatewayClient.inquire(PROVIDER_TX_ID))
                    .thenReturn(new PgInquiryResult(PROVIDER_TX_ID, ORDER_ID, AMOUNT, "DONE"));

            // when & then
            assertThatThrownBy(() -> sut.cancelDeposit(DEPOSIT_ID, "사유"))
                    .isInstanceOf(RuntimeException.class);

            // then: 상태 확인용 조회가 호출됨
            verify(paymentGatewayClient).inquire(PROVIDER_TX_ID);
        }
    }
}