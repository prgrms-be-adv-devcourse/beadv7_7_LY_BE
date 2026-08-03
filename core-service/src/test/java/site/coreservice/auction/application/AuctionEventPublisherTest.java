package site.coreservice.auction.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import site.common.event.Event;
import site.common.event.EventPublisher;
import site.coreservice.auction.domain.Auction;
import site.coreservice.auction.domain.AuctionSchedule;
import site.coreservice.auction.domain.AuctionStatus;
import site.common.event.contract.AuctionWonEvent;
import site.coreservice.auction.domain.HighestBid;
import site.coreservice.auction.domain.ItemCondition;
import site.coreservice.auction.domain.ItemInfo;
import site.coreservice.auction.domain.Money;
import site.coreservice.auction.domain.Period;
import site.coreservice.auction.domain.Pricing;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuctionEventPublisherTest {

    @Mock
    private EventPublisher eventPublisher;

    @Test
    void publishWon_Auction는_경매_정보를_그대로_담아_이벤트를_발행한다() {
        final HighestBid highestBid = HighestBid.of(Money.of(15_000L), 99L, 1L);
        final Auction auction = registerRunningAuction(highestBid, List.of("1.png", "2.png"));
        ReflectionTestUtils.setField(auction, "id", 10L);

        final AuctionEventPublisher auctionEventPublisher = new AuctionEventPublisher(
            eventPublisher);
        auctionEventPublisher.publishWon(auction);

        final ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventPublisher).publish(captor.capture());

        final AuctionWonEvent event = (AuctionWonEvent) captor.getValue();
        assertThat(event.getAuctionId()).isEqualTo(10L);
        assertThat(event.getProductId()).isEqualTo(auction.getProductId());
        assertThat(event.getWinnerId()).isEqualTo(99L);
        assertThat(event.getSellerId()).isEqualTo(auction.getSellerId());
        assertThat(event.getItemCondition()).isEqualTo(ItemCondition.MINT.name());
        assertThat(event.getFirstImageUrl()).isEqualTo("1.png");
        assertThat(event.getWinningPrice()).isEqualTo(Money.of(15_000L).getValue());
    }

    @Test
    void publishWon_Auction는_이미지가_없으면_firstImageUrl이_null이다() {
        final HighestBid highestBid = HighestBid.of(Money.of(15_000L), 99L, 1L);
        final Auction auction = registerRunningAuction(highestBid, null);
        ReflectionTestUtils.setField(auction, "id", 11L);

        final AuctionEventPublisher auctionEventPublisher = new AuctionEventPublisher(
            eventPublisher);
        auctionEventPublisher.publishWon(auction);

        final ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventPublisher).publish(captor.capture());

        final AuctionWonEvent event = (AuctionWonEvent) captor.getValue();
        assertThat(event.getFirstImageUrl()).isNull();
    }


    @Test
    void publishAuctionClosed은_입찰이_있으면_낙찰_이벤트를_발행한다() {
        final Auction auction = registerRunningAuction(HighestBid.of(Money.of(15_000L), 99L, 1L),
            null);
        ReflectionTestUtils.setField(auction, "id", 13L);

        final AuctionEventPublisher auctionEventPublisher = new AuctionEventPublisher(
            eventPublisher);
        auctionEventPublisher.publishAuctionClosed(auction);

        final ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(AuctionWonEvent.class);
    }

    private Auction registerRunningAuction(final HighestBid highestBid,
        final List<String> imageUrls) {
        final LocalDateTime startAt = LocalDateTime.now().minusHours(3);
        final LocalDateTime endAt = LocalDateTime.now().minusMinutes(1);
        final ItemInfo itemInfo = ItemInfo.of(ItemCondition.MINT, "이벤트 발행 테스트용 상품 설명입니다.",
            imageUrls);
        final Pricing pricing = Pricing.of(Money.of(1_000L), Money.of(100L), Money.of(3_000L));
        final AuctionSchedule schedule = AuctionSchedule.of(Period.of(startAt, endAt), false, null);
        return Auction.of(1L, 2L, itemInfo, pricing, schedule, AuctionStatus.RUNNING, highestBid);
    }
}
