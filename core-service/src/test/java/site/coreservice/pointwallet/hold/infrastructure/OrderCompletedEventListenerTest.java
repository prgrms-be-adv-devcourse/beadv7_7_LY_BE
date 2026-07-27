package site.coreservice.pointwallet.hold.infrastructure;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.coreservice.global.event.OrderCompletedEvent;
import site.coreservice.pointwallet.hold.application.HoldService;
import site.coreservice.pointwallet.hold.exception.HoldErrorCode;
import site.coreservice.pointwallet.hold.exception.HoldException;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderCompletedEventListener")
class OrderCompletedEventListenerTest {

    @Mock
    private HoldService holdService;

    private OrderCompletedEventListener sut;

    private static final Long AUCTION_ID = 5001L;

    @BeforeEach
    void setUp() {
        sut = new OrderCompletedEventListener(holdService);
    }

    private OrderCompletedEvent event() {
        return new OrderCompletedEvent(1L, AUCTION_ID, 456L, 789L, BigDecimal.valueOf(15_000), LocalDateTime.now());
    }

    @Test
    @DisplayName("정상 케이스면 consume을 호출한다")
    void handle_정상케이스() {
        // when
        sut.handle(event());

        // then
        verify(holdService).consume(AUCTION_ID);
    }

    @Test
    @DisplayName("HOLD_NOT_FOUND면 예외를 삼키고 조용히 스킵한다")
    void handle_홀드없으면_스킵() {
        // given
        doThrow(new HoldException(HoldErrorCode.HOLD_NOT_FOUND)).when(holdService).consume(AUCTION_ID);

        // when & then
        assertThatCode(() -> sut.handle(event())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("HOLD_NOT_FOUND가 아닌 다른 예외는 그대로 다시 던진다")
    void handle_다른예외는_전파() {
        // given
        doThrow(new HoldException(HoldErrorCode.WALLET_NOT_FOUND)).when(holdService).consume(AUCTION_ID);

        // when & then
        assertThatThrownBy(() -> sut.handle(event()))
                .isInstanceOf(HoldException.class)
                .extracting(e -> ((HoldException) e).getErrorCode())
                .isEqualTo(HoldErrorCode.WALLET_NOT_FOUND);
    }
}