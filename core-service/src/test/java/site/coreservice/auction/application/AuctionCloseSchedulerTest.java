package site.coreservice.auction.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import site.coreservice.auction.domain.Auction;
import site.coreservice.auction.domain.AuctionRepository;
import site.coreservice.auction.domain.AuctionSchedule;
import site.coreservice.auction.domain.AuctionStatus;
import site.coreservice.auction.domain.ItemCondition;
import site.coreservice.auction.domain.ItemInfo;
import site.coreservice.auction.domain.Money;
import site.coreservice.auction.domain.Period;
import site.coreservice.auction.domain.Pricing;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuctionCloseSchedulerTest {

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private AuctionScheduleService auctionScheduleService;

    @Mock
    private AuctionEventPublisher auctionEventPublisher;

    @Test
    void closeEndedAuctions은_대상_경매마다_closeAuction을_위임하고_결과를_이벤트로_발행한다() {
        final Auction a = runningAuction(1L);
        final Auction b = runningAuction(2L);
        when(auctionRepository.findAllRunningToEnd(any())).thenReturn(List.of(a, b));
        when(auctionScheduleService.closeAuction(1L)).thenReturn(a);
        when(auctionScheduleService.closeAuction(2L)).thenReturn(b);

        final AuctionCloseScheduler scheduler =
            new AuctionCloseScheduler(auctionRepository, auctionScheduleService,
                auctionEventPublisher);
        scheduler.closeEndedAuctions();

        verify(auctionScheduleService).closeAuction(1L);
        verify(auctionScheduleService).closeAuction(2L);
        verify(auctionEventPublisher).publishAuctionClosed(a);
        verify(auctionEventPublisher).publishAuctionClosed(b);
    }

    @Test
    void closeEndedAuctions은_한_건이_실패해도_나머지는_계속_처리한다() {
        final Auction a = runningAuction(1L);
        final Auction b = runningAuction(2L);
        when(auctionRepository.findAllRunningToEnd(any())).thenReturn(List.of(a, b));
        doThrow(new RuntimeException("boom")).when(auctionScheduleService).closeAuction(1L);
        when(auctionScheduleService.closeAuction(2L)).thenReturn(b);

        final AuctionCloseScheduler scheduler =
            new AuctionCloseScheduler(auctionRepository, auctionScheduleService,
                auctionEventPublisher);
        scheduler.closeEndedAuctions();

        verify(auctionScheduleService).closeAuction(1L);
        verify(auctionScheduleService).closeAuction(2L);
        verify(auctionEventPublisher).publishAuctionClosed(b);
    }

    @Test
    void closeEndedAuctions은_마감_1분_전_시각을_기준으로_조회한다() {
        when(auctionRepository.findAllRunningToEnd(any())).thenReturn(List.of());

        final LocalDateTime before = LocalDateTime.now();
        final AuctionCloseScheduler scheduler =
            new AuctionCloseScheduler(auctionRepository, auctionScheduleService,
                auctionEventPublisher);
        scheduler.closeEndedAuctions();

        final ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(auctionRepository).findAllRunningToEnd(captor.capture());

        final LocalDateTime threshold = captor.getValue();
        final Duration diff = Duration.between(before.minusMinutes(1), threshold).abs();
        assertThat(diff).isLessThan(Duration.ofSeconds(5));
    }

    private Auction runningAuction(final Long id) {
        final LocalDateTime startAt = LocalDateTime.now().minusHours(3);
        final LocalDateTime endAt = LocalDateTime.now().minusMinutes(1);
        final ItemInfo itemInfo = ItemInfo.of(ItemCondition.MINT, "스케줄러 위임 테스트용 상품 설명입니다.", null);
        final Pricing pricing = Pricing.of(Money.of(1_000L), Money.of(100L), Money.of(3_000L));
        final AuctionSchedule schedule = AuctionSchedule.of(Period.of(startAt, endAt), false, null);
        final Auction auction = Auction.of(1L, 2L, itemInfo, pricing, schedule,
            AuctionStatus.RUNNING, null);
        ReflectionTestUtils.setField(auction, "id", id);
        return auction;
    }
}
