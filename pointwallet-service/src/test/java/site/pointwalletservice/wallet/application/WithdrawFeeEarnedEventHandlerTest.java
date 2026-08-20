package site.pointwalletservice.wallet.application;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.pointwalletservice.ledger.application.PointTransactionService;
import site.pointwalletservice.ledger.domain.PointTransactionType;
import site.pointwalletservice.shared.Money;
import site.pointwalletservice.shared.PlatformAccount;
import site.pointwalletservice.withdraw.domain.event.WithdrawFeeEarnedEvent;

@ExtendWith(MockitoExtension.class)
@DisplayName("WithdrawFeeEarnedEventHandler")
class WithdrawFeeEarnedEventHandlerTest {

    @Mock
    private WalletService walletService;

    @Mock
    private PointTransactionService pointTransactionService;

    private WithdrawFeeEarnedEventHandler sut;

    private static final Long WITHDRAW_ID = 1L;
    private static final Long PLATFORM_WALLET_ID = 999L;

    @BeforeEach
    void setUp() {
        sut = new WithdrawFeeEarnedEventHandler(walletService, pointTransactionService);
    }

    @Test
    @DisplayName("처음 받는 이벤트면 플랫폼 계정에 수수료를 적립하고 원장에 기록한다")
    void handle_처음받는이벤트면_플랫폼계정에_적립된다() {
        // given
        WithdrawFeeEarnedEvent event = new WithdrawFeeEarnedEvent(WITHDRAW_ID, BigDecimal.valueOf(2_000));
        when(pointTransactionService.existsForRelatedId(WITHDRAW_ID, PointTransactionType.FEE_INCOME))
                .thenReturn(false);
        when(walletService.charge(eq(PlatformAccount.PLATFORM_USER_ID), any(Money.class)))
                .thenReturn(new WalletBalanceResult(PLATFORM_WALLET_ID, Money.of(2_000)));

        // when
        sut.handle(event);

        // then
        verify(walletService).charge(PlatformAccount.PLATFORM_USER_ID, Money.of(2_000));
        verify(pointTransactionService).record(
                PLATFORM_WALLET_ID, PointTransactionType.FEE_INCOME, Money.of(2_000), Money.of(2_000), WITHDRAW_ID
        );
    }

    @Test
    @DisplayName("이미 처리된(중복 전달된) 이벤트면 적립 없이 건너뛴다 — at-least-once 재전송 대비 멱등 처리")
    void handle_이미처리된이벤트면_건너뛴다() {
        // given
        WithdrawFeeEarnedEvent event = new WithdrawFeeEarnedEvent(WITHDRAW_ID, BigDecimal.valueOf(2_000));
        when(pointTransactionService.existsForRelatedId(WITHDRAW_ID, PointTransactionType.FEE_INCOME))
                .thenReturn(true);

        // when
        sut.handle(event);

        // then
        verify(walletService, never()).charge(any(), any());
        verify(pointTransactionService, never()).record(any(), any(), any(), any(), any());
    }
}