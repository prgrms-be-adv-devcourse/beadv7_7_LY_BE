package site.coreservice.auction.domain;

import java.math.BigDecimal;
import lombok.Getter;
import site.common.event.Event;

@Getter
public class AuctionWonEvent extends Event {

    private final Long auctionId;
    private final Long productId;
    private final Long winnerId;
    private final Long sellerId;
    private final ItemCondition itemCondition;
    private final String firstImageUrl;
    private final BigDecimal winningPrice;

    public AuctionWonEvent(
        final Long auctionId,
        final Long productId,
        final Long winnerId,
        final Long sellerId,
        final ItemCondition itemCondition,
        final String firstImageUrl,
        final BigDecimal winningPrice
    ) {
        this.auctionId = auctionId;
        this.productId = productId;
        this.winnerId = winnerId;
        this.sellerId = sellerId;
        this.itemCondition = itemCondition;
        this.firstImageUrl = firstImageUrl;
        this.winningPrice = winningPrice;
    }

    // Kafka 사용 시 토픽으로 사용
    @Override
    public String getEventType() {
        return "auction.won";
    }
}
