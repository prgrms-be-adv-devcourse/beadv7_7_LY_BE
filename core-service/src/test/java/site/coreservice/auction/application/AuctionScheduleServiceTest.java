package site.coreservice.auction.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.coreservice.auction.application.port.AuctionSearchViewRepository;
import site.coreservice.auction.domain.Auction;
import site.coreservice.auction.domain.AuctionRepository;
import site.coreservice.auction.domain.AuctionSchedule;
import site.coreservice.auction.domain.AuctionStatus;
import site.coreservice.auction.domain.HighestBid;
import site.coreservice.auction.domain.ItemCondition;
import site.coreservice.auction.domain.ItemInfo;
import site.coreservice.auction.domain.Money;
import site.coreservice.auction.domain.Period;
import site.coreservice.auction.domain.Pricing;

import java.time.LocalDateTime;
import java.util.Optional;
import site.coreservice.auction.exception.AuctionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuctionScheduleServiceTest {

    @Mock
    private AuctionRepository auctionRepository;

    @Test
    void startAuction은_RUNNING으로_전이하고_해당_경매를_반환한다() {
        final Auction auction = registerScheduledAuction();
        when(auctionRepository.findById(auction.getId())).thenReturn(Optional.of(auction));
        when(auctionRepository.save(auction)).thenReturn(auction);

        final AuctionScheduleService service =
            new AuctionScheduleService(auctionRepository);
        final Auction result = service.startAuction(auction.getId());

        assertThat(result).isSameAs(auction);
        assertThat(result.getStatus()).isEqualTo(AuctionStatus.RUNNING);
        verify(auctionRepository).save(auction);
    }

    @Test
    void startAuction은_존재하지_않으면_예외를_던진다() {
        when(auctionRepository.findById(999L)).thenReturn(Optional.empty());

        final AuctionScheduleService service =
            new AuctionScheduleService(auctionRepository);

        assertThatThrownBy(() -> service.startAuction(999L)).isInstanceOf(
            AuctionException.class);
    }

    @Test
    void 입찰이_있으면_ENDED_WON으로_전이하고_해당_경매를_반환한다() {
        final Auction auction = registerRunningAuction(HighestBid.of(Money.of(15_000L), 99L, 1L));
        when(auctionRepository.findById(auction.getId())).thenReturn(Optional.of(auction));
        when(auctionRepository.save(auction)).thenReturn(auction);

        final AuctionScheduleService service =
            new AuctionScheduleService(auctionRepository);
        final Auction result = service.closeAuction(auction.getId());

        assertThat(result).isSameAs(auction);
        assertThat(result.getStatus()).isEqualTo(AuctionStatus.ENDED_WON);
        verify(auctionRepository).save(auction);
    }

    @Test
    void 입찰이_없으면_ENDED_FAILED로_전이하고_해당_경매를_반환한다() {
        final Auction auction = registerRunningAuction(null);
        when(auctionRepository.findById(auction.getId())).thenReturn(Optional.of(auction));
        when(auctionRepository.save(auction)).thenReturn(auction);

        final AuctionScheduleService service =
            new AuctionScheduleService(auctionRepository);
        final Auction result = service.closeAuction(auction.getId());

        assertThat(result).isSameAs(auction);
        assertThat(result.getStatus()).isEqualTo(AuctionStatus.ENDED_FAILED);
        verify(auctionRepository).save(auction);
    }

    @Test
    void closeAuction은_존재하지_않으면_예외를_던진다() {
        when(auctionRepository.findById(999L)).thenReturn(Optional.empty());

        final AuctionScheduleService service =
            new AuctionScheduleService(auctionRepository);

        assertThatThrownBy(() -> service.closeAuction(999L)).isInstanceOf(
            AuctionException.class);
    }

    private Auction registerScheduledAuction() {
        final LocalDateTime startAt = LocalDateTime.now().minusMinutes(1);
        final LocalDateTime endAt = startAt.plusHours(2);
        final ItemInfo itemInfo = ItemInfo.of(ItemCondition.MINT, "시작 서비스 테스트용 상품 설명입니다.", null);
        final Pricing pricing = Pricing.of(Money.of(1_000L), Money.of(100L), Money.of(3_000L));
        final AuctionSchedule schedule = AuctionSchedule.of(Period.of(startAt, endAt), false, null);
        return Auction.of(1L, 2L, itemInfo, pricing, schedule, AuctionStatus.SCHEDULED, null);
    }

    private Auction registerRunningAuction(final HighestBid highestBid) {
        final LocalDateTime startAt = LocalDateTime.now().minusHours(3);
        final LocalDateTime endAt = LocalDateTime.now().minusMinutes(1);
        final ItemInfo itemInfo = ItemInfo.of(ItemCondition.MINT, "마감 서비스 테스트용 상품 설명입니다.", null);
        final Pricing pricing = Pricing.of(Money.of(1_000L), Money.of(100L), Money.of(3_000L));
        final AuctionSchedule schedule = AuctionSchedule.of(Period.of(startAt, endAt), false, null);
        return Auction.of(1L, 2L, itemInfo, pricing, schedule, AuctionStatus.RUNNING, highestBid);
    }
}
