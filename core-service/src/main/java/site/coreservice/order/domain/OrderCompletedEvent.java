package site.coreservice.order.domain;

import java.math.BigDecimal;
import lombok.Getter;
import site.common.event.Event;

@Getter
public class OrderCompletedEvent extends Event {

    private final Long orderId;
    private final Long auctionId;
    private final Long buyerId;
    private final Long sellerId;
    private final BigDecimal finalBidPrice;

    public OrderCompletedEvent(
        final Long orderId,
        final Long auctionId,
        final Long buyerId,
        final Long sellerId,
        final BigDecimal finalBidPrice
    ) {
        this.orderId = orderId;
        this.auctionId = auctionId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.finalBidPrice = finalBidPrice;
    }

    @Override
    public String getEventType() {
        return "order.completed";
    }
}
