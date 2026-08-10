package site.fulfillmentservice.order.application;

import org.springframework.stereotype.Component;
import site.common.event.EventPublisher;
import site.common.event.contract.OrderCancelledEvent;
import site.common.event.contract.OrderCompletedEvent;
import site.common.event.contract.OrderRefundedEvent;
import site.fulfillmentservice.order.domain.Order;

@Component
public class OrderEventPublisher {

    private final EventPublisher eventPublisher;

    public OrderEventPublisher(final EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishCancelled(final Order order) {
        eventPublisher.publish(
            OrderCancelledEvent.builder()
                .orderId(order.getId())
                .auctionId(order.getAuctionId())
                .buyerId(order.getBuyerId())
                .build());
    }

    public void publishCompleted(final Order order) {
        eventPublisher.publish(
            OrderCompletedEvent.builder()
                .orderId(order.getId())
                .auctionId(order.getAuctionId())
                .buyerId(order.getBuyerId())
                .sellerId(order.getSellerId())
                .finalBidPrice(order.getFinalBidPrice())
                .completedAt(order.getCompletedAt())
                .build());
    }

    public void publishRefunded(final Order order) {
        eventPublisher.publish(
            OrderRefundedEvent.builder()
                .orderId(order.getId())
                .auctionId(order.getAuctionId())
                .buyerId(order.getBuyerId())
                .build());
    }
}
