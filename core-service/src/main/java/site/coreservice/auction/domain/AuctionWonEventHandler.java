package site.coreservice.auction.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import site.common.event.EventHandler;

// TODO: 낙찰 이벤트 처리 예시(주문 생성 처리 참고용) - 이후 삭제 #16
@Slf4j
@Component
public class AuctionWonEventHandler implements EventHandler<AuctionWonEvent> {

    @Override
    public void handle(final AuctionWonEvent event) {
        log.info(
            "AuctionWonEvent 처리: auctionId={}, productId={}, winnerId={}, sellerId={}, winningPrice={}",
            event.getAuctionId(),
            event.getProductId(),
            event.getWinnerId(),
            event.getSellerId(),
            event.getWinningPrice()
        );
    }
}
