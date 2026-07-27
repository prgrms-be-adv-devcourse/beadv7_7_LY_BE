package site.coreservice.settlement.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import site.coreservice.global.event.OrderCompletedEvent;
import site.coreservice.settlement.application.OrderCompletedEventHandler;

@Component("settlementOrderCompletedEventListener")
@RequiredArgsConstructor
public class OrderCompletedEventListener {

    private final OrderCompletedEventHandler orderCompletedEventHandler;

    @EventListener
    public void handle(final OrderCompletedEvent event) {
        orderCompletedEventHandler.handle(event);
    }
}
