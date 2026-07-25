package site.coreservice.auction.application;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;
import site.common.event.EventPublisher;
import site.coreservice.auction.domain.Auction;
import site.coreservice.global.event.AuctionWonEvent;
import site.coreservice.auction.domain.HighestBid;
import site.coreservice.auction.domain.ItemCondition;

@Component
public class AuctionEventPublisher {

    private final EventPublisher eventPublisher;

    public AuctionEventPublisher(final EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    // Auction의 hasBid()로 낙찰/유찰 판단하여 알맞은 이벤트 발행
    public void publishAuctionClosed(final Auction auction) {
        if (auction.hasBid()) {
            publishWon(auction);
        }
    }

    public void publishWon(final Auction auction) {
        final HighestBid highestBid = auction.getHighestBid();
        final String firstImageUrl = firstImageUrl(auction);

        publishWon(
            auction.getId(),
            auction.getProductId(),
            highestBid.getBidderId(),
            auction.getSellerId(),
            auction.getItemInfo().getCondition(),
            firstImageUrl,
            highestBid.getAmount().getValue()
        );
    }

    public void publishWon(
        final Long auctionId,
        final Long productId,
        final Long winnerId,
        final Long sellerId,
        final ItemCondition itemCondition,
        final String firstImageUrl,
        final BigDecimal winningPrice
    ) {
        eventPublisher.publish(
            new AuctionWonEvent(auctionId, productId, winnerId, sellerId, itemCondition.name(),
                firstImageUrl, winningPrice));
    }

    private String firstImageUrl(final Auction auction) {
        final List<String> imageUrls = auction.getItemInfo().getImageUrls();
        return (imageUrls == null || imageUrls.isEmpty()) ? null : imageUrls.getFirst();
    }
}
