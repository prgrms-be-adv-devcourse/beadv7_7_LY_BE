package site.coreservice.auction.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.coreservice.auction.domain.Auction;
import site.coreservice.auction.domain.AuctionPolicy;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuctionStartSchedulerTest {

    @Mock
    private AuctionRepository auctionRepository;

    @Test
    void startScheduledAuctions_수행_후_RUNNING_상태_저장_확인() {
        final Auction auction = registerAuction();
        when(auctionRepository.findAllScheduledToStart(any())).thenReturn(List.of(auction));

        final AuctionStartScheduler scheduler = new AuctionStartScheduler(auctionRepository);
        scheduler.startScheduledAuctions();

        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.RUNNING);
        verify(auctionRepository).save(auction);
    }

    @Test
    void startScheduledAuctions은_시작_단위_절반만큼_이른_시각을_기준으로_조회한다() {
        when(auctionRepository.findAllScheduledToStart(any())).thenReturn(List.of());

        final LocalDateTime before = LocalDateTime.now();
        final AuctionStartScheduler scheduler = new AuctionStartScheduler(auctionRepository);
        scheduler.startScheduledAuctions();

        final ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(auctionRepository).findAllScheduledToStart(captor.capture());

        final LocalDateTime threshold = captor.getValue();
        final long expectedEarlyMinutes = AuctionPolicy.TIME_UNIT_MINUTES / 2L;
        final Duration diff = Duration.between(before.plusMinutes(expectedEarlyMinutes), threshold)
            .abs();
        assertThat(diff).isLessThan(Duration.ofSeconds(5));
    }

    @Test
    void startScheduledAuctions은_대상이_없으면_아무것도_저장하지_않는다() {
        when(auctionRepository.findAllScheduledToStart(any())).thenReturn(List.of());

        final AuctionStartScheduler scheduler = new AuctionStartScheduler(auctionRepository);
        scheduler.startScheduledAuctions();

        verify(auctionRepository, org.mockito.Mockito.never()).save(any());
    }

    private Auction registerAuction() {
        final LocalDateTime startAt = LocalDateTime.now().minusMinutes(1);
        final LocalDateTime endAt = startAt.plusHours(2);
        final ItemInfo itemInfo = ItemInfo.of(ItemCondition.MINT, "스케줄러 테스트용 상품 설명입니다.", null);
        final Pricing pricing = Pricing.of(Money.of(1_000L), Money.of(100L), Money.of(3_000L));
        final AuctionSchedule schedule = AuctionSchedule.of(Period.of(startAt, endAt), false, null);
        return Auction.register(1L, 2L, itemInfo, pricing, schedule);
    }
}
