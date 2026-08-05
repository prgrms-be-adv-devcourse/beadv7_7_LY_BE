package site.coreservice.pointwallet.wallet.application;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.common.event.contract.SettlementConfirmedEvent;
import site.coreservice.pointwallet.ledger.application.PointTransactionService;
import site.coreservice.pointwallet.ledger.domain.PointTransactionType;
import site.coreservice.pointwallet.shared.Money;

@ExtendWith(MockitoExtension.class)
@DisplayName("SettlementConfirmedEventHandler")
class SettlementConfirmedEventHandlerTest {

    @Mock
    private WalletService walletService;

    @Mock
    private PointTransactionService pointTransactionService;

    private SettlementConfirmedEventHandler sut;

    private static final Long SETTLEMENT_BATCH_ID = 10L;
    private static final Long SELLER_ID = 1L;
    private static final Long WALLET_ID = 100L;

    @BeforeEach
    void setUp() {
        sut = new SettlementConfirmedEventHandler(walletService, pointTransactionService);
    }

    @Test
    @DisplayName("정산 확정 이벤트를 받으면 판매자 지갑에 정산금을 입금하고 원장에 기록한다")
    void handle_정산확정시_판매자지갑에_입금되고_원장에_기록된다() {
        // given
        SettlementConfirmedEvent event = SettlementConfirmedEvent.builder()
                .settlementBatchId(SETTLEMENT_BATCH_ID)
                .sellerId(SELLER_ID)
                .totalAmount(BigDecimal.valueOf(50_000))
                .confirmedAt(LocalDateTime.now())
                .build();
        WalletBalanceResult result = new WalletBalanceResult(WALLET_ID, Money.of(50_000));
        when(walletService.charge(eq(SELLER_ID), any(Money.class))).thenReturn(result);

        // when
        sut.handle(event);

        // then
        verify(walletService).charge(SELLER_ID, Money.of(50_000));
        verify(pointTransactionService).record(
                WALLET_ID, PointTransactionType.SETTLEMENT_PAYOUT, Money.of(50_000),
                Money.of(50_000), SETTLEMENT_BATCH_ID
        );
    }

    @Test
    @DisplayName("판매자가 지갑을 개설한 적 없어도 charge()로 자동 개설되어 입금된다")
    void handle_지갑이_없던_판매자도_자동개설되어_입금된다() {
        // given
        SettlementConfirmedEvent event = SettlementConfirmedEvent.builder()
                .settlementBatchId(SETTLEMENT_BATCH_ID)
                .sellerId(SELLER_ID)
                .totalAmount(BigDecimal.valueOf(30_000))
                .confirmedAt(LocalDateTime.now())
                .build();
        WalletBalanceResult result = new WalletBalanceResult(WALLET_ID, Money.of(30_000));
        when(walletService.charge(eq(SELLER_ID), any(Money.class))).thenReturn(result);

        // when
        sut.handle(event);

        // then — credit()이 아니라 charge()를 호출했는지가 핵심(지갑 없어도 예외 없이 개설됨)
        verify(walletService).charge(SELLER_ID, Money.of(30_000));
    }
}