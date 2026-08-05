package site.coreservice.settlement.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import site.common.event.EventHandler;
import site.common.event.contract.OrderCompletedEvent;

@Component("settlementOrderCompletedEventHandler")
@RequiredArgsConstructor
public class OrderCompletedEventHandler implements EventHandler<OrderCompletedEvent> {

    private final SettlementItemService settlementItemService;

    @Override
    public void handle(final OrderCompletedEvent event) {
        settlementItemService.createSettlementItem(event);
    }
}
