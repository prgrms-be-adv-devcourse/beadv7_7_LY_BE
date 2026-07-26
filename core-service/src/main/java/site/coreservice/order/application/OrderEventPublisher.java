package site.coreservice.order.application;

import org.springframework.stereotype.Component;
import site.common.event.EventPublisher;
import site.coreservice.global.event.OrderCancelledEvent;
import site.coreservice.global.event.OrderCompletedEvent;
import site.coreservice.order.domain.Order;

@Component
public class OrderEventPublisher {

    private final EventPublisher eventPublisher;

    public OrderEventPublisher(final EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishCancelled(final Order order) {
        eventPublisher.publish(
            new OrderCancelledEvent(order.getId(), order.getAuctionId(), order.getBuyerId()));
    }

    public void publishCompleted(final Order order) {
        eventPublisher.publish(
            new OrderCompletedEvent(order.getId(), order.getAuctionId(), order.getBuyerId(),
                order.getSellerId(), order.getFinalBidPrice(), order.getCompletedAt()));
    }
}
