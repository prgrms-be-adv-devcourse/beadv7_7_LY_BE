package site.coreservice.product.infrastructure;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.coreservice.global.event.OrderCompletedEvent;
import site.coreservice.product.application.PriceHistoryRecordService;
import site.coreservice.product.exception.AuctionContractViolationException;
import site.coreservice.product.exception.PriceHistoryAuctionNotFoundException;

@ExtendWith(MockitoExtension.class)
class PriceHistoryOrderCompletedListenerTest {

    @Mock
    private PriceHistoryRecordService priceHistoryRecordService;

    @InjectMocks
    private PriceHistoryOrderCompletedListener listener;

    private OrderCompletedEvent event(Long auctionId, LocalDateTime completedAt) {
        return new OrderCompletedEvent(1L, auctionId, 2L, 3L, BigDecimal.valueOf(15_000), completedAt);
    }

    @Test
    @DisplayName("이벤트의 경매 id와 완료시각을 서비스에 그대로 전달한다")
    void handle_서비스에_위임() {
        // given
        LocalDateTime completedAt = LocalDateTime.of(2026, 7, 27, 10, 0);

        // when
        listener.handleOrderCompletedEvent(event(1010L, completedAt));

        // then
        verify(priceHistoryRecordService).recordConfirmedTrade(1010L, completedAt);
    }

    @Test
    @DisplayName("경매 id가 비어 있으면 서비스를 부르지 않고 끝낸다")
    void handle_auctionId_null_스킵() {
        // given: OrderCompletedEvent는 생성자에 null 검증이 없어 이런 값이 올 수 있다
        // when
        listener.handleOrderCompletedEvent(event(null, LocalDateTime.now()));

        // then
        verify(priceHistoryRecordService, never()).recordConfirmedTrade(any(), any());
    }

    @Test
    @DisplayName("완료시각이 비어 있으면 서비스를 부르지 않고 끝낸다")
    void handle_completedAt_null_스킵() {
        // given-when
        listener.handleOrderCompletedEvent(event(1010L, null));

        // then
        verify(priceHistoryRecordService, never()).recordConfirmedTrade(any(), any());
    }

    @Test
    @DisplayName("데이터 불일치 예외를 삼켜 발행자에게 전파하지 않는다")
    void handle_데이터불일치_삼킴() {
        // given
        willThrow(new PriceHistoryAuctionNotFoundException(1010L))
                .given(priceHistoryRecordService).recordConfirmedTrade(any(), any());

        // when-then: 예외가 새면 발행자 응답이 오염되고 같은 커밋의 다른 리스너 실행까지 끊긴다
        assertThatCode(() -> listener.handleOrderCompletedEvent(event(1010L, LocalDateTime.now())))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("계약 위반 예외를 삼켜 발행자에게 전파하지 않는다")
    void handle_계약위반_삼킴() {
        // given
        willThrow(new AuctionContractViolationException("낙찰가가 정수가 아닙니다: 15000.75"))
                .given(priceHistoryRecordService).recordConfirmedTrade(any(), any());

        // when-then
        assertThatCode(() -> listener.handleOrderCompletedEvent(event(1010L, LocalDateTime.now())))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("일시 장애로 보이는 예외도 삼켜 발행자에게 전파하지 않는다")
    void handle_일시장애_삼킴() {
        // given
        willThrow(new IllegalStateException("DB 연결 실패"))
                .given(priceHistoryRecordService).recordConfirmedTrade(any(), any());

        // when-then
        assertThatCode(() -> listener.handleOrderCompletedEvent(event(1010L, LocalDateTime.now())))
                .doesNotThrowAnyException();
    }
}
