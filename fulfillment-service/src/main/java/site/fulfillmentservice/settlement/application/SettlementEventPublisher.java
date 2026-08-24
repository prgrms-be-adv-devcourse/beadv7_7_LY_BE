package site.fulfillmentservice.settlement.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import site.common.event.contract.EventType;
import site.common.event.contract.SettlementConfirmedEvent;
import site.fulfillmentservice.outbox.application.OutboxEventStore;
import site.fulfillmentservice.settlement.domain.SettlementBatch;

@Component
@RequiredArgsConstructor
public class SettlementEventPublisher {

    private final OutboxEventStore outboxEventStore;

    public void publishConfirmed(final SettlementBatch batch) {
        outboxEventStore.store(
            EventType.SETTLEMENT_CONFIRMED_EVENT.getValue(),
            batch.getSellerId().toString(),
            SettlementConfirmedEvent.builder()
                .settlementBatchId(batch.getId())
                .sellerId(batch.getSellerId())
                .totalAmount(batch.getTotalAmount().getValue())
                .confirmedAt(batch.getConfirmedAt())
                .build());
    }
}
