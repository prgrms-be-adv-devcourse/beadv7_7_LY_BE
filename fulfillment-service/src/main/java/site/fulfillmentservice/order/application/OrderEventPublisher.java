package site.fulfillmentservice.order.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import site.common.event.contract.EventType;
import site.common.event.contract.OrderCancelledEvent;
import site.common.event.contract.OrderCompletedEvent;
import site.common.event.contract.OrderRefundedEvent;
import site.fulfillmentservice.order.domain.Order;
import site.fulfillmentservice.outbox.application.OutboxEventStore;

@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final OutboxEventStore outboxEventStore;

    public void publishCancelled(final Order order) {
        outboxEventStore.store(
            EventType.ORDER_CANCELLED_EVENT.getValue(),
            order.getId().toString(),
            OrderCancelledEvent.builder()
                .orderId(order.getId())
                .auctionId(order.getAuctionId())
                .buyerId(order.getBuyerId())
                .build());
    }

    public void publishCompleted(final Order order) {
        outboxEventStore.store(
            EventType.ORDER_COMPLETED_EVENT.getValue(),
            order.getId().toString(),
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
        outboxEventStore.store(
            EventType.ORDER_REFUNDED_EVENT.getValue(),
            order.getId().toString(),
            OrderRefundedEvent.builder()
                .orderId(order.getId())
                .auctionId(order.getAuctionId())
                .buyerId(order.getBuyerId())
                .build());
    }
}
