package site.coreservice.order.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import site.common.event.contract.AuctionWonEvent;
import site.coreservice.order.application.AuctionWonEventHandler;

@Component("orderAuctionWonEventListener")
@RequiredArgsConstructor
public class AuctionWonEventListener {

    private final AuctionWonEventHandler auctionWonEventHandler;

    @KafkaListener(topics = "#{T(site.common.event.contract.EventType).AUCTION_WON_EVENT.getValue()}",
            groupId = "order-service")
    public void handle(final AuctionWonEvent event) {
        auctionWonEventHandler.handle(event);
    }
}
