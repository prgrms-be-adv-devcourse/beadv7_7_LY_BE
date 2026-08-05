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
public class SettlementConfirmedEvent extends Event {

    private final Long settlementBatchId;
    private final Long sellerId;
    private final BigDecimal totalAmount;
    private final LocalDateTime confirmedAt;

    @Builder
    @JsonCreator
    private SettlementConfirmedEvent(
        @JsonProperty("eventId") final UUID eventId,
        @JsonProperty("occurredAt") final LocalDateTime occurredAt,
        @JsonProperty("settlementBatchId") final Long settlementBatchId,
        @JsonProperty("sellerId") final Long sellerId,
        @JsonProperty("totalAmount") final BigDecimal totalAmount,
        @JsonProperty("confirmedAt") final LocalDateTime confirmedAt
    ) {
        super(eventId, occurredAt);
        this.settlementBatchId = settlementBatchId;
        this.sellerId = sellerId;
        this.totalAmount = totalAmount;
        this.confirmedAt = confirmedAt;
    }

    @Override
    public String getEventType() {
        return EventType.SETTLEMENT_CONFIRMED_EVENT.getValue();
    }
}
