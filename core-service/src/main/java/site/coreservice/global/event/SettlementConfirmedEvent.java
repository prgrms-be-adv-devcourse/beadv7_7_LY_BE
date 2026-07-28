package site.coreservice.global.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import site.common.event.Event;

@Getter
public class SettlementConfirmedEvent extends Event {

    private final Long settlementBatchId;
    private final Long sellerId;
    private final BigDecimal totalAmount;
    private final LocalDateTime confirmedAt;

    public SettlementConfirmedEvent(
        final Long settlementBatchId,
        final Long sellerId,
        final BigDecimal totalAmount,
        final LocalDateTime confirmedAt
    ) {
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
