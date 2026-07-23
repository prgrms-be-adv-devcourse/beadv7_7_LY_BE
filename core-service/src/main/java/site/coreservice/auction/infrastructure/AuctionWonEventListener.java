package site.coreservice.auction.infrastructure;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import site.coreservice.auction.domain.AuctionWonEvent;
import site.coreservice.auction.application.AuctionWonEventHandler;

// TODO: 삭제 #16, 이벤트 확인 및 리스너 구현 예시용 임시 어댑터
@Component
public class AuctionWonEventListener {

    private final AuctionWonEventHandler auctionWonEventHandler;

    public AuctionWonEventListener(final AuctionWonEventHandler auctionWonEventHandler) {
        this.auctionWonEventHandler = auctionWonEventHandler;
    }

    @Async
    @EventListener
    public void handle(final AuctionWonEvent event) {
        auctionWonEventHandler.handle(event);
    }
}
