package site.coreservice.pointwallet.hold.infrastructure;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.coreservice.global.event.OrderCancelledEvent;
import site.coreservice.pointwallet.hold.application.HoldService;
import site.coreservice.pointwallet.hold.exception.HoldErrorCode;
import site.coreservice.pointwallet.hold.exception.HoldException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@ExtendWith(MockitoExtension.class)
@DisplayName("OrderCancelledEventListener")
class OrderCancelledEventListenerTest {

    @Mock
    private HoldService holdService;

    private OrderCancelledEventListener sut;

    private static final Long AUCTION_ID = 5001L;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        sut = new OrderCancelledEventListener(holdService);
    }

    @Test
    @DisplayName("정상 케이스면 release를 호출한다")
    void handle_정상케이스() {
        // given
        OrderCancelledEvent event = new OrderCancelledEvent(1L, AUCTION_ID, 456L);

        // when
        sut.handle(event);

        // then
        verify(holdService).release(AUCTION_ID);
    }

    @Test
    @DisplayName("HOLD_NOT_FOUND면 예외를 삼키고 조용히 스킵한다")
    void handle_홀드없으면_스킵() {
        // given
        OrderCancelledEvent event = new OrderCancelledEvent(1L, AUCTION_ID, 456L);
        doThrow(new HoldException(HoldErrorCode.HOLD_NOT_FOUND)).when(holdService).release(AUCTION_ID);

        // when & then (예외가 밖으로 안 나가야 함)
        org.assertj.core.api.Assertions.assertThatCode(() -> sut.handle(event)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("HOLD_NOT_FOUND가 아닌 다른 예외는 그대로 다시 던진다")
    void handle_다른예외는_전파() {
        // given
        OrderCancelledEvent event = new OrderCancelledEvent(1L, AUCTION_ID, 456L);
        doThrow(new HoldException(HoldErrorCode.WALLET_NOT_FOUND)).when(holdService).release(AUCTION_ID);

        // when & then
        assertThatThrownBy(() -> sut.handle(event))
                .isInstanceOf(HoldException.class)
                .extracting(e -> ((HoldException) e).getErrorCode())
                .isEqualTo(HoldErrorCode.WALLET_NOT_FOUND);
    }
}