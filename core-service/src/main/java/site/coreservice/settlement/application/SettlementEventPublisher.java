package site.coreservice.settlement.application;

import org.springframework.stereotype.Component;
import site.common.event.EventPublisher;
import site.common.event.contract.SettlementConfirmedEvent;
import site.coreservice.settlement.domain.SettlementBatch;

@Component
public class SettlementEventPublisher {

    private final EventPublisher eventPublisher;

    public SettlementEventPublisher(final EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishConfirmed(final SettlementBatch batch) {
        eventPublisher.publish(
            SettlementConfirmedEvent.builder()
                .settlementBatchId(batch.getId())
                .sellerId(batch.getSellerId())
                .totalAmount(batch.getTotalAmount().getValue())
                .confirmedAt(batch.getConfirmedAt())
                .build());
    }
}
