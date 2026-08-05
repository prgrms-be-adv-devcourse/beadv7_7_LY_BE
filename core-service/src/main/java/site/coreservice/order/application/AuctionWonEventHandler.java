package site.coreservice.order.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import site.common.event.EventHandler;
import site.common.event.contract.AuctionWonEvent;

@Component("orderAuctionWonEventHandler")
@RequiredArgsConstructor
public class AuctionWonEventHandler implements EventHandler<AuctionWonEvent> {

    private final OrderService orderService;

    @Override
    public void handle(final AuctionWonEvent event) {
        orderService.createOrder(event);
    }
}
