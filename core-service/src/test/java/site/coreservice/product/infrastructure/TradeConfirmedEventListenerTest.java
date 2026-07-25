package site.coreservice.product.infrastructure;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.coreservice.product.application.PriceHistoryRecordService;
import site.coreservice.product.domain.TradeConfirmedEvent;

@ExtendWith(MockitoExtension.class)
class TradeConfirmedEventListenerTest {

    @Mock
    private PriceHistoryRecordService priceHistoryRecordService;

    @InjectMocks
    private TradeConfirmedEventListener listener;

    @Test
    @DisplayName("이벤트의 경매 id와 확정시각을 서비스에 그대로 전달한다")
    void handle_서비스에_위임() {
        // given
        LocalDateTime confirmedAt = LocalDateTime.of(2026, 7, 24, 10, 0);
        TradeConfirmedEvent event = new TradeConfirmedEvent(1010L, confirmedAt);

        // when
        listener.handleTradeConfirmedEvent(event);

        // then
        verify(priceHistoryRecordService).recordConfirmedTrade(1010L, confirmedAt);
    }

    @Test
    @DisplayName("서비스가 예외를 던져도 리스너 밖으로 새어나가지 않는다")
    void handle_예외를_밖으로_전파하지_않음() {
        // given
        willThrow(new IllegalStateException("경매 없음"))
                .given(priceHistoryRecordService).recordConfirmedTrade(any(), any());
        TradeConfirmedEvent event = new TradeConfirmedEvent(1010L, LocalDateTime.now());

        // when-then: 예외가 새면 발행자 응답이 오염되고 같은 커밋의 다른 리스너 실행까지 끊긴다
        assertThatCode(() -> listener.handleTradeConfirmedEvent(event)).doesNotThrowAnyException();
    }
}
