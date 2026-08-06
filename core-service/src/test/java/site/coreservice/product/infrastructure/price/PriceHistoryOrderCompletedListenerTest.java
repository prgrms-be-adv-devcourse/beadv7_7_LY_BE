package site.coreservice.product.infrastructure.price;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import site.common.event.contract.OrderCompletedEvent;
import site.coreservice.product.application.price.PriceHistoryRecordService;
import site.coreservice.product.exception.AuctionContractViolationException;
import site.coreservice.product.exception.PriceHistoryAuctionNotClosedException;
import site.coreservice.product.exception.PriceHistoryAuctionNotFoundException;

@ExtendWith(MockitoExtension.class)
class PriceHistoryOrderCompletedListenerTest {

    @Mock
    private PriceHistoryRecordService priceHistoryRecordService;

    @InjectMocks
    private PriceHistoryOrderCompletedListener listener;

    private Logger listenerLogger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        listenerLogger = (Logger) LoggerFactory.getLogger(PriceHistoryOrderCompletedListener.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        listenerLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        listenerLogger.detachAppender(logAppender);
    }

    private OrderCompletedEvent event(Long auctionId, LocalDateTime completedAt) {
        return OrderCompletedEvent.builder()
                .orderId(1L)
                .auctionId(auctionId)
                .buyerId(2L)
                .sellerId(3L)
                .finalBidPrice(BigDecimal.valueOf(15_000))
                .completedAt(completedAt)
                .build();
    }

    private boolean hasMarker(String marker) {
        return logAppender.list.stream()
                .anyMatch(e -> e.getLevel() == Level.ERROR && e.getFormattedMessage().contains(marker));
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
        assertThat(logAppender.list).isEmpty();
    }

    @Test
    @DisplayName("경매 id가 비어 있으면 서비스를 부르지 않고 끝낸다")
    void handle_auctionId_null_스킵() {
        // given: OrderCompletedEvent는 생성자에 null 검증이 없어 이런 값이 올 수 있다
        // when
        listener.handleOrderCompletedEvent(event(null, LocalDateTime.now()));

        // then
        verify(priceHistoryRecordService, never()).recordConfirmedTrade(any(), any());
        assertThat(hasMarker("[시세적재-이벤트결함]")).isTrue();
    }

    @Test
    @DisplayName("완료시각이 비어 있으면 서비스를 부르지 않고 끝낸다")
    void handle_completedAt_null_스킵() {
        // given-when
        listener.handleOrderCompletedEvent(event(1010L, null));

        // then
        verify(priceHistoryRecordService, never()).recordConfirmedTrade(any(), any());
        assertThat(hasMarker("[시세적재-이벤트결함]")).isTrue();
    }

    @Test
    @DisplayName("경매를 찾을 수 없는 데이터 불일치 예외를 삼켜 발행자에게 전파하지 않는다")
    void handle_데이터불일치_경매없음_삼킴() {
        // given
        willThrow(new PriceHistoryAuctionNotFoundException(1010L))
                .given(priceHistoryRecordService).recordConfirmedTrade(any(), any());

        // when-then: 예외가 새면 발행자 응답이 오염되고 같은 커밋의 다른 리스너 실행까지 끊긴다
        assertThatCode(() -> listener.handleOrderCompletedEvent(event(1010L, LocalDateTime.now())))
                .doesNotThrowAnyException();
        assertThat(hasMarker("[시세적재-데이터불일치]")).isTrue();
    }

    @Test
    @DisplayName("경매가 아직 마감되지 않은 데이터 불일치 예외를 삼켜 발행자에게 전파하지 않는다")
    void handle_데이터불일치_미마감_삼킴() {
        // given
        willThrow(new PriceHistoryAuctionNotClosedException(1010L, "RUNNING"))
                .given(priceHistoryRecordService).recordConfirmedTrade(any(), any());

        // when-then
        assertThatCode(() -> listener.handleOrderCompletedEvent(event(1010L, LocalDateTime.now())))
                .doesNotThrowAnyException();
        assertThat(hasMarker("[시세적재-데이터불일치]")).isTrue();
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
        assertThat(hasMarker("[시세적재-계약위반]")).isTrue();
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
        assertThat(hasMarker("[시세적재-실패]")).isTrue();
    }
}
