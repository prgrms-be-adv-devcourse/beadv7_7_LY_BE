package site.common.event.contract;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import site.common.event.Event;

@Getter
public class OrderCompletedEvent extends Event {

    private final Long orderId;
    private final Long auctionId;
    private final Long buyerId;
    private final Long sellerId;
    private final BigDecimal finalBidPrice;
    private final LocalDateTime completedAt;

    @Builder
    private OrderCompletedEvent(
        final Long orderId,
        final Long auctionId,
        final Long buyerId,
        final Long sellerId,
        final BigDecimal finalBidPrice,
        final LocalDateTime completedAt
    ) {
        this.orderId = orderId;
        this.auctionId = auctionId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.finalBidPrice = finalBidPrice;
        this.completedAt = completedAt;
    }

    @Override
    public String getEventType() {
        return EventType.ORDER_COMPLETED_EVENT.getValue();
    }
}
