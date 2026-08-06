package site.coreservice.product.application.price;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.LocalDateTime;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import site.coreservice.product.application.port.AuctionSnapshotPort;
import site.coreservice.product.domain.price.ClosedAuction;
import site.coreservice.product.domain.price.MediaCondition;
import site.coreservice.product.domain.price.PriceHistory;
import site.coreservice.product.domain.price.PriceHistoryRepository;
import site.coreservice.product.exception.AuctionContractViolationException;
import site.coreservice.product.exception.PriceHistoryAuctionNotClosedException;
import site.coreservice.product.exception.PriceHistoryAuctionNotFoundException;

@ExtendWith(MockitoExtension.class)
class PriceHistoryRecordServiceTest {

    private static final LocalDateTime CLOSED_AT = LocalDateTime.of(2026, 7, 10, 20, 31);
    private static final LocalDateTime CONFIRMED_AT = LocalDateTime.of(2026, 7, 11, 10, 0);

    @Mock
    private AuctionSnapshotPort auctionSnapshotPort;

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    private PriceHistoryRecordService service;
    private Logger serviceLogger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        service = new PriceHistoryRecordService(auctionSnapshotPort, priceHistoryRepository, transactionManager);
        serviceLogger = (Logger) LoggerFactory.getLogger(PriceHistoryRecordService.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        serviceLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        serviceLogger.detachAppender(logAppender);
    }

    private ClosedAuction closedAuction() {
        return new ClosedAuction(1024L, 55L, MediaCondition.NEAR_MINT, 72000L, 7, CLOSED_AT, "ENDED_WON");
    }

    @Test
    @DisplayName("새 거래확정이면 경매 정보를 조회해 시세 기록을 저장한다")
    void record_새_거래는_저장() {
        // given
        given(priceHistoryRepository.findByAuctionId(1024L)).willReturn(Optional.empty());
        given(auctionSnapshotPort.findClosedAuction(1024L)).willReturn(Optional.of(closedAuction()));

        // when
        service.recordConfirmedTrade(1024L, CONFIRMED_AT);

        // then
        ArgumentCaptor<PriceHistory> captor = ArgumentCaptor.forClass(PriceHistory.class);
        verify(priceHistoryRepository).save(captor.capture());
        Assertions.assertThat(captor.getValue().getAuctionId()).isEqualTo(1024L);
        Assertions.assertThat(captor.getValue().getProductId()).isEqualTo(55L);
        Assertions.assertThat(captor.getValue().isOutlier()).isFalse();
    }

    @Test
    @DisplayName("이미 기록된 경매면 저장하지 않고 정상 종료한다")
    void record_중복이면_건너뜀() {
        // given
        given(priceHistoryRepository.findByAuctionId(1024L))
                .willReturn(Optional.of(PriceHistory.of(closedAuction(), CONFIRMED_AT)));

        // when-then
        assertThatCode(() -> service.recordConfirmedTrade(1024L, CONFIRMED_AT)).doesNotThrowAnyException();
        verify(priceHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 기록된 경매가 다른 확정시각으로 재도착하면 경고 로그를 남기고 건너뛴다")
    void record_확정시각_불일치_중복도_건너뜀() {
        // given: 확정시각 불일치 재도착 (경고 로그는 setUp이 부착한 appender가 수집)
        given(priceHistoryRepository.findByAuctionId(1024L))
                .willReturn(Optional.of(PriceHistory.of(closedAuction(), CONFIRMED_AT)));

        // when
        service.recordConfirmedTrade(1024L, CONFIRMED_AT.plusHours(3));

        // then: 저장은 건너뛰되 확정시각 불일치를 경고로 남긴다
        verify(priceHistoryRepository, never()).save(any());
        Assertions.assertThat(logAppender.list)
                .anyMatch(e -> e.getLevel() == Level.WARN && e.getFormattedMessage().contains("다른 확정시각"));
    }

    @Test
    @DisplayName("저장이 제약 위반으로 거부됐지만 재확인에 행이 있으면 동시 중복으로 보고 정상 종료한다")
    void record_동시_중복_경합은_건너뜀() {
        // given
        given(priceHistoryRepository.findByAuctionId(1024L)).willReturn(Optional.empty());
        given(auctionSnapshotPort.findClosedAuction(1024L)).willReturn(Optional.of(closedAuction()));
        given(priceHistoryRepository.save(any())).willThrow(new DataIntegrityViolationException("duplicate"));
        given(priceHistoryRepository.existsByAuctionId(1024L)).willReturn(true);

        // when-then
        assertThatCode(() -> service.recordConfirmedTrade(1024L, CONFIRMED_AT)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("제약 위반인데 재확인에도 행이 없으면 다른 원인이므로 예외를 다시 던진다")
    void record_원인불명_제약위반은_재던짐() {
        // given
        given(priceHistoryRepository.findByAuctionId(1024L)).willReturn(Optional.empty());
        given(auctionSnapshotPort.findClosedAuction(1024L)).willReturn(Optional.of(closedAuction()));
        given(priceHistoryRepository.save(any())).willThrow(new DataIntegrityViolationException("not null"));
        given(priceHistoryRepository.existsByAuctionId(1024L)).willReturn(false);

        // when-then
        assertThatThrownBy(() -> service.recordConfirmedTrade(1024L, CONFIRMED_AT))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("경매를 찾을 수 없으면 예외를 던진다")
    void record_경매_없음_예외() {
        // given
        given(priceHistoryRepository.findByAuctionId(1024L)).willReturn(Optional.empty());
        given(auctionSnapshotPort.findClosedAuction(1024L)).willReturn(Optional.empty());

        // when-then
        assertThatThrownBy(() -> service.recordConfirmedTrade(1024L, CONFIRMED_AT))
                .isInstanceOf(PriceHistoryAuctionNotFoundException.class);
        verify(priceHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("아직 마감되지 않은 경매면 예외를 던진다")
    void record_미마감_경매_예외() {
        // given
        ClosedAuction open = new ClosedAuction(1024L, 55L, MediaCondition.NEAR_MINT, 72000L, 7, CLOSED_AT, "RUNNING");
        given(priceHistoryRepository.findByAuctionId(1024L)).willReturn(Optional.empty());
        given(auctionSnapshotPort.findClosedAuction(1024L)).willReturn(Optional.of(open));

        // when-then
        assertThatThrownBy(() -> service.recordConfirmedTrade(1024L, CONFIRMED_AT))
                .isInstanceOf(PriceHistoryAuctionNotClosedException.class);
        verify(priceHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("경매 응답에 필수값이 빠져 있으면 계약 위반으로 올린다")
    void recordConfirmedTrade_필수값_누락_계약위반() {
        // given: 경매가 응답 필드명을 바꾸면 파싱은 성공하고 값만 null이 된다
        ClosedAuction 마감시각없음 = new ClosedAuction(1024L, 55L, MediaCondition.NEAR_MINT, 72000L, 7,
                null, "ENDED_WON");
        given(priceHistoryRepository.findByAuctionId(1024L)).willReturn(Optional.empty());
        given(auctionSnapshotPort.findClosedAuction(1024L)).willReturn(Optional.of(마감시각없음));

        // when-then: 재시도해도 같은 결과라 일시 장애 갈래로 가면 안 된다
        assertThatThrownBy(() -> service.recordConfirmedTrade(1024L, CONFIRMED_AT))
                .isInstanceOf(AuctionContractViolationException.class);
    }
}
