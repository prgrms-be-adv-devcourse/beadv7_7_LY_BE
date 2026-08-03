package site.common.event.contract;

import lombok.Builder;
import lombok.Getter;
import site.common.event.Event;

@Getter
public class OrderCancelledEvent extends Event {

    private final Long orderId;
    private final Long auctionId;
    private final Long buyerId;

    @Builder
    private OrderCancelledEvent(
        final Long orderId,
        final Long auctionId,
        final Long buyerId
    ) {
        this.orderId = orderId;
        this.auctionId = auctionId;
        this.buyerId = buyerId;
    }

    @Override
    public String getEventType() {
        return EventType.ORDER_CANCELLED_EVENT.getValue();
    }
}
