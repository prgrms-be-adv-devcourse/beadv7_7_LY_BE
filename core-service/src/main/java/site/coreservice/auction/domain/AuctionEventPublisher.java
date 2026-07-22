package site.coreservice.auction.domain;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import site.common.event.EventPublisher;

@Component
public class AuctionEventPublisher {

    private final EventPublisher eventPublisher;

    public AuctionEventPublisher(final EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishWon(
        final Long auctionId,
        final Long productId,
        final Long winnerId,
        final Long sellerId,
        final BigDecimal winningPrice
    ) {
        eventPublisher.publish(
            new AuctionWonEvent(auctionId, productId, winnerId, sellerId, winningPrice));
    }
}
