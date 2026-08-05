package site.coreservice.settlement.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import site.common.event.contract.OrderCompletedEvent;
import site.coreservice.settlement.application.OrderCompletedEventHandler;

@Component("settlementOrderCompletedEventListener")
@RequiredArgsConstructor
public class OrderCompletedEventListener {

    private final OrderCompletedEventHandler orderCompletedEventHandler;

    @KafkaListener(topics = "#{T(site.common.event.contract.EventType).ORDER_COMPLETED_EVENT.getValue()}",
            groupId = "settlement-service")
    public void handle(final OrderCompletedEvent event) {
        orderCompletedEventHandler.handle(event);
    }
}
