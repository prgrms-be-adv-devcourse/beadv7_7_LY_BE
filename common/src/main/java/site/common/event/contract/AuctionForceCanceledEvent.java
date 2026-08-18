package site.common.event.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import site.common.event.Event;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class AuctionForceCanceledEvent extends Event {
    private final Long auctionId;
    private final Long bidderId;

    @Builder
    @JsonCreator
    private AuctionForceCanceledEvent(
            @JsonProperty("eventId") final UUID eventId,
            @JsonProperty("occurredAt") final LocalDateTime occurredAt,
            @JsonProperty("auctionId") final Long auctionId,
            @JsonProperty("bidderId") final Long bidderId
    ) {
        super(eventId, occurredAt);
        this.auctionId = auctionId;
        this.bidderId = bidderId;
    }

    @Override
    public String getEventType() {
        return EventType.AUCTION_FORCE_CANCELED_EVENT.getValue();
    }
}
