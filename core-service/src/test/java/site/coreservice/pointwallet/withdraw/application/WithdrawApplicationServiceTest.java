package site.coreservice.pointwallet.withdraw.application;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import site.coreservice.pointwallet.ledger.application.PointTransactionService;
import site.coreservice.pointwallet.ledger.domain.PointTransactionType;
import site.coreservice.pointwallet.shared.Money;
import site.coreservice.pointwallet.shared.PlatformAccount;
import site.coreservice.pointwallet.wallet.application.WalletBalanceResult;
import site.coreservice.pointwallet.wallet.application.WalletService;
import site.coreservice.pointwallet.wallet.domain.InsufficientBalanceException;
import site.coreservice.pointwallet.wallet.exception.WalletNotFoundException;
import site.coreservice.pointwallet.withdraw.application.dto.WithdrawRequestResult;
import site.coreservice.pointwallet.withdraw.application.dto.WithdrawStatusResult;
import site.coreservice.pointwallet.withdraw.application.port.BankAccount;
import site.coreservice.pointwallet.withdraw.application.port.MemberBankAccountPort;
import site.coreservice.pointwallet.withdraw.domain.Withdraw;
import site.coreservice.pointwallet.withdraw.domain.WithdrawRepository;
import site.coreservice.pointwallet.withdraw.domain.WithdrawStatus;
import site.coreservice.pointwallet.withdraw.exception.WithdrawErrorCode;
import site.coreservice.pointwallet.withdraw.exception.WithdrawException;

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

    private WithdrawApplicationService sut;

    private static final Long USER_ID = 1L;
    private static final Long WALLET_ID = 100L;
    private static final Long PLATFORM_WALLET_ID = 999L;
    private static final Money AMOUNT = Money.of(100_000);
    private static final Money FEE_AMOUNT = Money.of(2_000);  // 100,000 * 2%
    private static final Money NET_AMOUNT = Money.of(98_000);
    private static final BankAccount BANK_ACCOUNT = new BankAccount("하나은행", "123-123456-12301", "홍길동");

    @BeforeEach
    void setUp() {
        sut = new WithdrawApplicationService(withdrawRepository, walletService, pointTransactionService, memberBankAccountPort);
    }

    @Nested
    @DisplayName("인출 신청 (requestWithdraw)")
    class RequestWithdraw {

        @Test
        @DisplayName("정상 흐름이면 수수료(2%, 내림)를 계산해 사용자 지갑 차감 + 플랫폼 계정 적립 + 즉시 SUCCESS 처리한다")
        void requestWithdraw_정상흐름() {
            // given
            when(memberBankAccountPort.getBankAccount(USER_ID)).thenReturn(Optional.of(BANK_ACCOUNT));
            when(walletService.deduct(USER_ID, AMOUNT))
                    .thenReturn(new WalletBalanceResult(WALLET_ID, Money.of(0)));
            when(walletService.charge(PlatformAccount.PLATFORM_USER_ID, FEE_AMOUNT))
                    .thenReturn(new WalletBalanceResult(PLATFORM_WALLET_ID, FEE_AMOUNT));
            when(withdrawRepository.save(any(Withdraw.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            WithdrawRequestResult result = sut.requestWithdraw(USER_ID, AMOUNT);

            // then
            assertThat(result.status()).isEqualTo(WithdrawStatus.SUCCESS);
            assertThat(result.feeAmount()).isEqualByComparingTo(FEE_AMOUNT.getValue());
            assertThat(result.netAmount()).isEqualByComparingTo(NET_AMOUNT.getValue());

            ArgumentCaptor<Withdraw> captor = ArgumentCaptor.forClass(Withdraw.class);
            verify(withdrawRepository).save(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
            assertThat(captor.getValue().getAmount()).isEqualTo(AMOUNT);

            verify(pointTransactionService).record(
                    WALLET_ID, PointTransactionType.WITHDRAW, AMOUNT, Money.of(0), captor.getValue().getId()
            );
            verify(pointTransactionService).record(
                    PLATFORM_WALLET_ID, PointTransactionType.FEE_INCOME, FEE_AMOUNT, FEE_AMOUNT, captor.getValue().getId()
            );
        }

        @Test
        @DisplayName("등록된 계좌가 없으면 BANK_ACCOUNT_NOT_FOUND를 던지고 지갑에는 손도 안 댄다")
        void requestWithdraw_계좌없으면_예외() {
            // given
            when(memberBankAccountPort.getBankAccount(USER_ID)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sut.requestWithdraw(USER_ID, AMOUNT))
                    .isInstanceOf(WithdrawException.class)
                    .extracting(e -> ((WithdrawException) e).getErrorCode())
                    .isEqualTo(WithdrawErrorCode.BANK_ACCOUNT_NOT_FOUND);

            verify(walletService, never()).deduct(any(), any());
            verify(withdrawRepository, never()).save(any());
        }

        @Test
        @DisplayName("지갑이 없으면 WALLET_NOT_FOUND로 번역된다")
        void requestWithdraw_지갑없으면_WALLET_NOT_FOUND() {
            // given
            when(memberBankAccountPort.getBankAccount(USER_ID)).thenReturn(Optional.of(BANK_ACCOUNT));
            when(walletService.deduct(USER_ID, AMOUNT)).thenThrow(new WalletNotFoundException());

            // when & then
            assertThatThrownBy(() -> sut.requestWithdraw(USER_ID, AMOUNT))
                    .isInstanceOf(WithdrawException.class)
                    .extracting(e -> ((WithdrawException) e).getErrorCode())
                    .isEqualTo(WithdrawErrorCode.WALLET_NOT_FOUND);

            verify(withdrawRepository, never()).save(any());
            verify(walletService, never()).charge(any(), any());
        }

        @Test
        @DisplayName("잔액이 부족하면 INSUFFICIENT_BALANCE로 번역된다")
        void requestWithdraw_잔액부족() {
            // given
            when(memberBankAccountPort.getBankAccount(USER_ID)).thenReturn(Optional.of(BANK_ACCOUNT));
            when(walletService.deduct(USER_ID, AMOUNT)).thenThrow(new InsufficientBalanceException());

            // when & then
            assertThatThrownBy(() -> sut.requestWithdraw(USER_ID, AMOUNT))
                    .isInstanceOf(WithdrawException.class)
                    .extracting(e -> ((WithdrawException) e).getErrorCode())
                    .isEqualTo(WithdrawErrorCode.INSUFFICIENT_BALANCE);

            verify(withdrawRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("인출 상태 조회 (getStatus)")
    class GetStatus {

        @Test
        @DisplayName("존재하면 상태를 반환한다")
        void getStatus_정상조회() {
            // given
            Withdraw withdraw = Withdraw.request(USER_ID, AMOUNT, FEE_AMOUNT, NET_AMOUNT);
            org.springframework.test.util.ReflectionTestUtils.setField(withdraw, "id", 1L);
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