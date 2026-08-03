package site.common.event.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
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
    @JsonCreator
    private OrderCompletedEvent(
        @JsonProperty("eventId") final UUID eventId,
        @JsonProperty("occurredAt") final LocalDateTime occurredAt,
        @JsonProperty("orderId") final Long orderId,
        @JsonProperty("auctionId") final Long auctionId,
        @JsonProperty("buyerId") final Long buyerId,
        @JsonProperty("sellerId") final Long sellerId,
        @JsonProperty("finalBidPrice") final BigDecimal finalBidPrice,
        @JsonProperty("completedAt") final LocalDateTime completedAt
    ) {
        super(eventId, occurredAt);
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
