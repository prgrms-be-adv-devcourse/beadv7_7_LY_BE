package site.auctionservice.application;

import org.springframework.stereotype.Component;
import site.common.event.EventPublisher;
import site.common.event.contract.AuctionForceCanceledEvent;
import site.common.event.contract.AuctionWonEvent;
import site.auctionservice.domain.Auction;
import site.auctionservice.domain.HighestBid;
import site.auctionservice.domain.ItemCondition;

import java.math.BigDecimal;
import java.util.List;

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
            AuctionWonEvent.builder()
                .auctionId(auctionId)
                .productId(productId)
                .winnerId(winnerId)
                .sellerId(sellerId)
                .itemCondition(itemCondition.name())
                .firstImageUrl(firstImageUrl)
                .winningPrice(winningPrice)
                .build());
    }

    private String firstImageUrl(final Auction auction) {
        final List<String> imageUrls = auction.getItemInfo().getImageUrls();
        return (imageUrls == null || imageUrls.isEmpty()) ? null : imageUrls.getFirst();
    }

    public void publishForceCanceled(final Long auctionId, final Long bidderId) {
        eventPublisher.publish(
                AuctionForceCanceledEvent.builder()
                        .auctionId(auctionId)
                        .bidderId(bidderId)
                        .build()
        );
    }
}
