package site.productservice.application.wishlist;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import site.common.event.EventPublisher;
import site.common.event.contract.WishlistChangedEvent;

@Slf4j
@Component
public class WishlistEventPublisher {

    private final EventPublisher eventPublisher;

    public WishlistEventPublisher(final EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishAdded(final Long memberId, final Long productId) {
        publishChanged(memberId, productId);
    }

    public void publishRemoved(final Long memberId, final Long productId) {
        publishChanged(memberId, productId);
    }

    private void publishChanged(final Long memberId, final Long productId) {
        try {
            eventPublisher.publish(
                WishlistChangedEvent.builder()
                    .memberId(memberId)
                    .build());
        } catch (final Exception e) {
            log.error("WishlistChangedEvent 발행 실패 — memberId: {}, productId: {}. ", memberId,
                productId, e);
        }
    }
}
