package site.pointwalletservice.wallet.application;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.common.event.contract.OrderRefundedEvent;
import site.pointwalletservice.ledger.application.PointTransactionService;
import site.pointwalletservice.ledger.domain.PointTransactionType;
import site.pointwalletservice.ledger.exception.LedgerErrorCode;
import site.pointwalletservice.ledger.exception.LedgerException;
import site.pointwalletservice.shared.Money;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderRefundedEventHandler")
class OrderRefundedEventHandlerTest {

    @Mock
    private WalletService walletService;

    @Mock
    private PointTransactionService pointTransactionService;

    private OrderRefundedEventHandler sut;

    private static final Long ORDER_ID = 1L;
    private static final Long AUCTION_ID = 5001L;
    private static final Long BUYER_ID = 456L;
    private static final Long WALLET_ID = 100L;
    private static final Money HOLD_AMOUNT = Money.of(15_000);

    @BeforeEach
    void setUp() {
        sut = new OrderRefundedEventHandler(walletService, pointTransactionService);
    }

    private OrderRefundedEvent buildEvent() {
        return OrderRefundedEvent.builder()
                .orderId(ORDER_ID)
                .auctionId(AUCTION_ID)
                .buyerId(BUYER_ID)
                .build();
    }

    @Test
    @DisplayName("처음 받는 이벤트면 원장에서 낙찰 홀드 금액을 찾아 구매자 지갑에 환불하고 REFUND로 기록한다")
    void handle_처음받는이벤트면_낙찰홀드금액만큼_환불된다() {
        // given
        OrderRefundedEvent event = buildEvent();
        when(pointTransactionService.existsForRelatedId(ORDER_ID, PointTransactionType.REFUND))
                .thenReturn(false);
        when(pointTransactionService.findLatestHoldAmountByAuctionId(AUCTION_ID))
                .thenReturn(Optional.of(HOLD_AMOUNT));
        when(walletService.credit(eq(BUYER_ID), any(Money.class)))
                .thenReturn(new WalletBalanceResult(WALLET_ID, HOLD_AMOUNT));

        // when
        sut.handle(event);

        // then
        verify(walletService).credit(BUYER_ID, HOLD_AMOUNT);
        verify(pointTransactionService).record(
                WALLET_ID, PointTransactionType.REFUND, HOLD_AMOUNT, HOLD_AMOUNT, ORDER_ID
        );
    }

    @Test
    @DisplayName("이미 처리된(중복 전달된) 이벤트면 환불 없이 건너뛴다 — at-least-once 재전송 대비 멱등 처리")
    void handle_이미처리된이벤트면_건너뛴다() {
        // given
        OrderRefundedEvent event = buildEvent();
        when(pointTransactionService.existsForRelatedId(ORDER_ID, PointTransactionType.REFUND))
                .thenReturn(true);

        // when
        sut.handle(event);

        // then
        verify(walletService, never()).credit(any(), any());
        verify(pointTransactionService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("해당 경매의 낙찰 홀드 원장을 찾을 수 없으면 AUCTION_HOLD_LEDGER_NOT_FOUND를 던지고 환불하지 않는다")
    void handle_낙찰홀드원장을_못찾으면_예외() {
        // given
        OrderRefundedEvent event = buildEvent();
        when(pointTransactionService.existsForRelatedId(ORDER_ID, PointTransactionType.REFUND))
                .thenReturn(false);
        when(pointTransactionService.findLatestHoldAmountByAuctionId(AUCTION_ID))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> sut.handle(event))
                .isInstanceOf(LedgerException.class)
                .extracting(e -> ((LedgerException) e).getErrorCode())
                .isEqualTo(LedgerErrorCode.AUCTION_HOLD_LEDGER_NOT_FOUND);

        verify(walletService, never()).credit(any(), any());
        verify(pointTransactionService, never()).record(any(), any(), any(), any(), any());
    }
}