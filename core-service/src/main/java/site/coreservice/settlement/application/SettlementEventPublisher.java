package site.coreservice.settlement.application;

import org.springframework.stereotype.Component;
import site.common.event.EventPublisher;
import site.coreservice.global.event.SettlementConfirmedEvent;
import site.coreservice.settlement.domain.SettlementBatch;

@Component
public class SettlementEventPublisher {

    private final EventPublisher eventPublisher;

    public SettlementEventPublisher(final EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishConfirmed(final SettlementBatch batch) {
        eventPublisher.publish(
            new SettlementConfirmedEvent(batch.getId(), batch.getSellerId(), batch.getTotalAmount().getValue(), batch.getConfirmedAt()));
    }
}
