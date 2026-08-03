package site.coreservice.order.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import site.common.event.contract.AuctionWonEvent;
import site.coreservice.order.application.AuctionWonEventHandler;

@Component("orderAuctionWonEventListener")
@RequiredArgsConstructor
public class AuctionWonEventListener {

    private final AuctionWonEventHandler auctionWonEventHandler;

    @EventListener
    public void handle(final AuctionWonEvent event) {
        auctionWonEventHandler.handle(event);
    }
}
