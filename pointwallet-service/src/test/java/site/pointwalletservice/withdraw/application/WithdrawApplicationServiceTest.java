package site.pointwalletservice.withdraw.application;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import site.pointwalletservice.ledger.application.PointTransactionService;
import site.pointwalletservice.ledger.domain.PointTransactionType;
import site.pointwalletservice.outbox.application.OutboxEventStore;
import site.pointwalletservice.shared.Money;
import site.pointwalletservice.shared.PlatformAccount;
import site.pointwalletservice.wallet.application.WalletBalanceResult;
import site.pointwalletservice.wallet.application.WalletService;
import site.pointwalletservice.wallet.domain.InsufficientBalanceException;
import site.pointwalletservice.wallet.exception.WalletLockFailedException;
import site.pointwalletservice.wallet.exception.WalletNotFoundException;
import site.pointwalletservice.withdraw.application.dto.WithdrawRequestResult;
import site.pointwalletservice.withdraw.application.dto.WithdrawStatusResult;
import site.pointwalletservice.withdraw.application.port.BankAccount;
import site.pointwalletservice.withdraw.application.port.MemberBankAccountPort;
import site.pointwalletservice.withdraw.domain.Withdraw;
import site.pointwalletservice.withdraw.domain.WithdrawRepository;
import site.pointwalletservice.withdraw.domain.WithdrawStatus;
import site.pointwalletservice.withdraw.domain.event.WithdrawFeeEarnedEvent;
import site.pointwalletservice.withdraw.exception.WithdrawErrorCode;
import site.pointwalletservice.withdraw.exception.WithdrawException;
import site.pointwalletservice.withdraw.exception.WithdrawLockContentionException;

@ExtendWith(MockitoExtension.class)
@DisplayName("WithdrawApplicationService")
class WithdrawApplicationServiceTest {

    @Mock
    private WithdrawRepository withdrawRepository;

    @Mock
    private WalletService walletService;

    @Mock
    private PointTransactionService pointTransactionService;

    @Mock
    private MemberBankAccountPort memberBankAccountPort;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private OutboxEventStore outboxEventStore;

    private WithdrawApplicationService sut;

    private static final Long USER_ID = 1L;
    private static final Long WALLET_ID = 100L;
    private static final Long WITHDRAW_ID = 1L;
    private static final Money AMOUNT = Money.of(100_000);
    private static final Money FEE_AMOUNT = Money.of(2_000);  // 100,000 * 2%
    private static final Money NET_AMOUNT = Money.of(98_000);
    private static final String IDEMPOTENCY_KEY = "test-idem-key-0001";
    private static final BankAccount BANK_ACCOUNT = new BankAccount("하나은행", "123-123456-12301", "홍길동");

    @BeforeEach
    void setUp() {
        // TransactionTemplate.execute()가 실제 트랜잭션 없이 콜백을 즉시 실행하도록 스텁 —
        // DepositApplicationServiceTest와 동일한 패턴.
        org.mockito.Mockito.lenient().doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction((TransactionStatus) null);
        }).when(transactionTemplate).execute(any());

        sut = new WithdrawApplicationService(
                withdrawRepository, walletService, pointTransactionService,
                memberBankAccountPort, transactionTemplate, outboxEventStore
        );
    }

    /** save() 시점에 ID를 채워준다 — requestWithdraw()가 이후 withdraw.getId()를 outbox 파티션 키로 쓰기 때문에 필요. */
    private void stubSaveWithId(Long id) {
        when(withdrawRepository.save(any(Withdraw.class))).thenAnswer(invocation -> {
            Withdraw w = invocation.getArgument(0);
            ReflectionTestUtils.setField(w, "id", id);
            return w;
        });
    }

    @Nested
    @DisplayName("계좌 검증 (validateBankAccount) — 재시도 루프 바깥에서 1회만 호출되는 부분")
    class ValidateBankAccount {

        @Test
        @DisplayName("등록된 계좌가 있으면 조용히 통과한다")
        void 계좌있으면_통과() {
            // given
            when(memberBankAccountPort.getBankAccount(USER_ID)).thenReturn(Optional.of(BANK_ACCOUNT));

            // when & then — 예외 없이 끝나야 한다
            sut.validateBankAccount(USER_ID);
        }

        @Test
        @DisplayName("등록된 계좌가 없으면 BANK_ACCOUNT_NOT_FOUND를 던진다")
        void 계좌없으면_예외() {
            // given
            when(memberBankAccountPort.getBankAccount(USER_ID)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sut.validateBankAccount(USER_ID))
                    .isInstanceOf(WithdrawException.class)
                    .extracting(e -> ((WithdrawException) e).getErrorCode())
                    .isEqualTo(WithdrawErrorCode.BANK_ACCOUNT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("멱등키 사전 조회 (findExisting) — userId를 항상 함께 대조한다")
    class FindExisting {

        @Test
        @DisplayName("같은 (userId, idempotencyKey)로 저장된 건이 있으면 그 건을 반환한다")
        void 존재하면_반환() {
            // given
            Withdraw existing = Withdraw.request(USER_ID, IDEMPOTENCY_KEY, AMOUNT, FEE_AMOUNT, NET_AMOUNT);
            when(withdrawRepository.findByUserIdAndIdempotencyKey(USER_ID, IDEMPOTENCY_KEY))
                    .thenReturn(Optional.of(existing));

            // when
            Optional<Withdraw> result = sut.findExisting(USER_ID, IDEMPOTENCY_KEY);

            // then
            assertThat(result).contains(existing);
        }

        @Test
        @DisplayName("없으면 빈 Optional을 반환한다")
        void 없으면_빈값() {
            // given
            when(withdrawRepository.findByUserIdAndIdempotencyKey(USER_ID, IDEMPOTENCY_KEY))
                    .thenReturn(Optional.empty());

            // when
            Optional<Withdraw> result = sut.findExisting(USER_ID, IDEMPOTENCY_KEY);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("지갑 차감 트랜잭션 (executeDeductionAndOutbox) — 락 경합 시 재시도되는 부분")
    class ExecuteDeductionAndOutbox {

        @Test
        @DisplayName("정상 흐름이면 수수료(2%, 내림)를 계산해 사용자 지갑만 차감하고, " +
                "플랫폼 계정 적립은 WithdrawFeeEarnedEvent를 Outbox에 저장하는 것으로 넘긴다")
        void 정상흐름() {
            // given — 계좌 조회 호출 자체가 없다(이 메서드 책임이 아니므로 memberBankAccountPort는 스텁도 안 함)
            when(walletService.deduct(USER_ID, AMOUNT))
                    .thenReturn(new WalletBalanceResult(WALLET_ID, Money.of(0)));
            stubSaveWithId(WITHDRAW_ID);

            // when
            Withdraw withdraw = sut.executeDeductionAndOutbox(USER_ID, AMOUNT, IDEMPOTENCY_KEY);

            // then
            assertThat(withdraw.getStatus()).isEqualTo(WithdrawStatus.SUCCESS);

            ArgumentCaptor<Withdraw> captor = ArgumentCaptor.forClass(Withdraw.class);
            verify(withdrawRepository).save(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
            assertThat(captor.getValue().getAmount()).isEqualTo(AMOUNT);
            assertThat(captor.getValue().getIdempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);

            verify(pointTransactionService).record(
                    WALLET_ID, PointTransactionType.WITHDRAW, AMOUNT, Money.of(0), WITHDRAW_ID
            );

            // 플랫폼 계정 charge()는 더 이상 이 트랜잭션 안에서 직접 호출되지 않는다
            verify(walletService, never()).charge(any(), any());

            // 대신 같은 트랜잭션 안에서 WithdrawFeeEarnedEvent가 Outbox에 저장된다 —
            // 파티션 키는 withdrawId가 아니라 PLATFORM_USER_ID로 고정된다(모든 인출 수수료 이벤트가
            // 한 파티션에 몰려 순차 처리되도록 보장하기 위함).
            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(outboxEventStore).store(
                    org.mockito.ArgumentMatchers.eq(WithdrawFeeEarnedEvent.TOPIC),
                    org.mockito.ArgumentMatchers.eq(PlatformAccount.PLATFORM_USER_ID.toString()),
                    eventCaptor.capture()
            );
            assertThat(eventCaptor.getValue()).isInstanceOf(WithdrawFeeEarnedEvent.class);
            WithdrawFeeEarnedEvent event = (WithdrawFeeEarnedEvent) eventCaptor.getValue();
            assertThat(event.withdrawId()).isEqualTo(WITHDRAW_ID);
            assertThat(event.feeAmount()).isEqualByComparingTo(FEE_AMOUNT.getValue());

            // 계좌 조회는 이 메서드 책임이 아니므로 한 번도 호출되지 않는다
            verify(memberBankAccountPort, never()).getBankAccount(any());
        }

        @Test
        @DisplayName("지갑이 없으면 WALLET_NOT_FOUND로 번역된다")
        void 지갑없으면_WALLET_NOT_FOUND() {
            // given
            when(walletService.deduct(USER_ID, AMOUNT)).thenThrow(new WalletNotFoundException());

            // when & then
            assertThatThrownBy(() -> sut.executeDeductionAndOutbox(USER_ID, AMOUNT, IDEMPOTENCY_KEY))
                    .isInstanceOf(WithdrawException.class)
                    .extracting(e -> ((WithdrawException) e).getErrorCode())
                    .isEqualTo(WithdrawErrorCode.WALLET_NOT_FOUND);

            verify(withdrawRepository, never()).save(any());
            verify(outboxEventStore, never()).store(any(), any(), any());
        }

        @Test
        @DisplayName("잔액이 부족하면 INSUFFICIENT_BALANCE로 번역된다")
        void 잔액부족() {
            // given
            when(walletService.deduct(USER_ID, AMOUNT)).thenThrow(new InsufficientBalanceException());

            // when & then
            assertThatThrownBy(() -> sut.executeDeductionAndOutbox(USER_ID, AMOUNT, IDEMPOTENCY_KEY))
                    .isInstanceOf(WithdrawException.class)
                    .extracting(e -> ((WithdrawException) e).getErrorCode())
                    .isEqualTo(WithdrawErrorCode.INSUFFICIENT_BALANCE);

            verify(withdrawRepository, never()).save(any());
        }

        @Test
        @DisplayName("사용자 본인 지갑 락 경합이면 WithdrawLockContentionException으로 번역된다 " +
                "(RetryingWithdrawService가 이 타입만 골라 재시도한다)")
        void 지갑락경합() {
            // given
            when(walletService.deduct(USER_ID, AMOUNT)).thenThrow(new WalletLockFailedException());

            // when & then
            assertThatThrownBy(() -> sut.executeDeductionAndOutbox(USER_ID, AMOUNT, IDEMPOTENCY_KEY))
                    .isInstanceOf(WithdrawLockContentionException.class)
                    .extracting(e -> ((WithdrawException) e).getErrorCode())
                    .isEqualTo(WithdrawErrorCode.LOCK_ACQUISITION_FAILED);

            verify(withdrawRepository, never()).save(any());
            verify(outboxEventStore, never()).store(any(), any(), any());
        }

        @Test
        @DisplayName("커밋 시점에 (user_id, idempotency_key) 유니크 제약 위반이 나면 " +
                "(동시에 같은 키로 들어온 다른 요청이 레이스로 먼저 이겼다는 뜻) " +
                "재시도하지 않고 이미 저장된 기존 건을 조회해 반환한다")
        void 유니크제약위반시_기존건으로_수렴한다() {
            // given: 트랜잭션 커밋 자체가 유니크 제약 위반으로 실패한다고 가정
            // (실제로는 이 트랜잭션 안에서 지갑 차감도 함께 자동 롤백된 상태)
            doThrow(new DataIntegrityViolationException("duplicate key"))
                    .when(transactionTemplate).execute(any());

            Withdraw existing = Withdraw.request(USER_ID, IDEMPOTENCY_KEY, AMOUNT, FEE_AMOUNT, NET_AMOUNT);
            ReflectionTestUtils.setField(existing, "id", WITHDRAW_ID);
            existing.complete();
            when(withdrawRepository.findByUserIdAndIdempotencyKey(USER_ID, IDEMPOTENCY_KEY))
                    .thenReturn(Optional.of(existing));

            // when
            Withdraw result = sut.executeDeductionAndOutbox(USER_ID, AMOUNT, IDEMPOTENCY_KEY);

            // then — 레이스에서 진 요청은 자기가 새로 저장을 시도하지 않고, 이긴 쪽의 결과를 그대로 받는다
            assertThat(result).isSameAs(existing);
            verify(withdrawRepository).findByUserIdAndIdempotencyKey(USER_ID, IDEMPOTENCY_KEY);
        }

        @Test
        @DisplayName("유니크 제약 위반인데 기존 건 조회마저 실패하면(비정상 상황) WITHDRAW_NOT_FOUND를 던진다")
        void 유니크제약위반인데_기존건도_없으면_예외() {
            // given
            doThrow(new DataIntegrityViolationException("duplicate key"))
                    .when(transactionTemplate).execute(any());
            when(withdrawRepository.findByUserIdAndIdempotencyKey(USER_ID, IDEMPOTENCY_KEY))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sut.executeDeductionAndOutbox(USER_ID, AMOUNT, IDEMPOTENCY_KEY))
                    .isInstanceOf(WithdrawException.class)
                    .extracting(e -> ((WithdrawException) e).getErrorCode())
                    .isEqualTo(WithdrawErrorCode.WITHDRAW_NOT_FOUND);
        }
    }

    // TODO 계좌 검증 롤백 및 계좌 사용 로직 추가 #351
    @Nested
    @DisplayName("인출 신청 (requestWithdraw) — validateBankAccount + executeDeductionAndOutbox 조합")
    class RequestWithdraw {

        @Test
        @DisplayName("계좌 검증 후 지갑 차감까지 이어져서 최종 결과를 반환한다")
        void requestWithdraw_정상흐름() {
            // given
            //when(memberBankAccountPort.getBankAccount(USER_ID)).thenReturn(Optional.of(BANK_ACCOUNT));
            when(walletService.deduct(USER_ID, AMOUNT))
                    .thenReturn(new WalletBalanceResult(WALLET_ID, Money.of(0)));
            stubSaveWithId(WITHDRAW_ID);

            // when
            WithdrawRequestResult result = sut.requestWithdraw(USER_ID, AMOUNT, IDEMPOTENCY_KEY);

            // then
            assertThat(result.status()).isEqualTo(WithdrawStatus.SUCCESS);
            assertThat(result.feeAmount()).isEqualByComparingTo(FEE_AMOUNT.getValue());
            assertThat(result.netAmount()).isEqualByComparingTo(NET_AMOUNT.getValue());
        }

//        @Test
//        @DisplayName("등록된 계좌가 없으면 트랜잭션 자체를 시작하지 않는다")
//        void requestWithdraw_계좌없으면_트랜잭션_미시작() {
//            // given
//            when(memberBankAccountPort.getBankAccount(USER_ID)).thenReturn(Optional.empty());
//
//            // when & then
//            assertThatThrownBy(() -> sut.requestWithdraw(USER_ID, AMOUNT, IDEMPOTENCY_KEY))
//                    .isInstanceOf(WithdrawException.class)
//                    .extracting(e -> ((WithdrawException) e).getErrorCode())
//                    .isEqualTo(WithdrawErrorCode.BANK_ACCOUNT_NOT_FOUND);
//
//            verify(transactionTemplate, never()).execute(any());
//            verify(walletService, never()).deduct(any(), any());
//            verify(withdrawRepository, never()).save(any());
//        }
    }

    @Nested
    @DisplayName("인출 상태 조회 (getStatus)")
    class GetStatus {

        @Test
        @DisplayName("존재하면 상태를 반환한다")
        void getStatus_정상조회() {
            // given
            Withdraw withdraw = Withdraw.request(USER_ID, IDEMPOTENCY_KEY, AMOUNT, FEE_AMOUNT, NET_AMOUNT);
            ReflectionTestUtils.setField(withdraw, "id", 1L);
            when(withdrawRepository.findById(1L)).thenReturn(Optional.of(withdraw));

            // when
            WithdrawStatusResult result = sut.getStatus(1L);

            // then
            assertThat(result.withdrawRequestId()).isEqualTo(1L);
            assertThat(result.status()).isEqualTo(WithdrawStatus.PENDING);
        }

        @Test
        @DisplayName("존재하지 않으면 WITHDRAW_NOT_FOUND를 던진다")
        void getStatus_없으면_예외() {
            // given
            when(withdrawRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sut.getStatus(999L))
                    .isInstanceOf(WithdrawException.class)
                    .extracting(e -> ((WithdrawException) e).getErrorCode())
                    .isEqualTo(WithdrawErrorCode.WITHDRAW_NOT_FOUND);
        }
    }
}