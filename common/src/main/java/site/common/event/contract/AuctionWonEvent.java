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
public class AuctionWonEvent extends Event {

    private final Long auctionId;
    private final Long productId;
    private final Long winnerId;
    private final Long sellerId;
    private final String itemCondition;
    private final String firstImageUrl;
    private final BigDecimal winningPrice;

    @Builder
    @JsonCreator
    private AuctionWonEvent(
        @JsonProperty("eventId") final UUID eventId,
        @JsonProperty("occurredAt") final LocalDateTime occurredAt,
        @JsonProperty("auctionId") final Long auctionId,
        @JsonProperty("productId") final Long productId,
        @JsonProperty("winnerId") final Long winnerId,
        @JsonProperty("sellerId") final Long sellerId,
        @JsonProperty("itemCondition") final String itemCondition,
        @JsonProperty("firstImageUrl") final String firstImageUrl,
        @JsonProperty("winningPrice") final BigDecimal winningPrice
    ) {
        super(eventId, occurredAt);
        this.auctionId = auctionId;
        this.productId = productId;
        this.winnerId = winnerId;
        this.sellerId = sellerId;
        this.itemCondition = itemCondition;
        this.firstImageUrl = firstImageUrl;
        this.winningPrice = winningPrice;
    }

    // Kafka 사용 시 토픽으로 사용
    @Override
    public String getEventType() {
        return EventType.AUCTION_WON_EVENT.getValue();
    }
}
