package site.common.event.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import site.common.event.Event;

@Getter
public class OrderCancelledEvent extends Event {

    private final Long orderId;
    private final Long auctionId;
    private final Long buyerId;

    @Builder
    @JsonCreator
    private OrderCancelledEvent(
        @JsonProperty("eventId") final UUID eventId,
        @JsonProperty("occurredAt") final LocalDateTime occurredAt,
        @JsonProperty("orderId") final Long orderId,
        @JsonProperty("auctionId") final Long auctionId,
        @JsonProperty("buyerId") final Long buyerId
    ) {
        super(eventId, occurredAt);
        this.orderId = orderId;
        this.auctionId = auctionId;
        this.buyerId = buyerId;
    }

    @Override
    public String getEventType() {
        return EventType.ORDER_CANCELLED_EVENT.getValue();
    }
}
