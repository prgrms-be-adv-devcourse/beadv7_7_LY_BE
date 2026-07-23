package site.coreservice.auction.domain;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.common.event.EventPublisher;
import site.coreservice.auction.application.AuctionEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuctionEventPublisherTest {

    @Mock
    private EventPublisher eventPublisher;

    @Test
    void 낙찰_이벤트_발행() {
        final AuctionEventPublisher auctionEventPublisher = new AuctionEventPublisher(
            eventPublisher);
        final BigDecimal winningPrice = BigDecimal.valueOf(50_000L);

        auctionEventPublisher.publishWon(1L, 2L, 3L, 4L, winningPrice);

        final ArgumentCaptor<AuctionWonEvent> captor = ArgumentCaptor.forClass(
            AuctionWonEvent.class);
        verify(eventPublisher).publish(captor.capture());

        final AuctionWonEvent event = captor.getValue();
        assertThat(event.getAuctionId()).isEqualTo(1L);
        assertThat(event.getProductId()).isEqualTo(2L);
        assertThat(event.getWinnerId()).isEqualTo(3L);
        assertThat(event.getSellerId()).isEqualTo(4L);
        assertThat(event.getWinningPrice()).isEqualTo(winningPrice);
    }
}
