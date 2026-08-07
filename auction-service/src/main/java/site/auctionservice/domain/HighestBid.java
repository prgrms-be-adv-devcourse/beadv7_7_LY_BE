package site.auctionservice.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HighestBid {
    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "highest_bid_amount"))
    private Money amount;

    @Column(name = "highest_bidder_id")
    private Long bidderId;

    @Column(name = "highest_bid_id")
    private Long bidId;

    private HighestBid(Money amount, Long bidderId, Long bidId) {
        this.amount = amount;
        this.bidderId = bidderId;
        this.bidId = bidId;
    }

    public static HighestBid of(Money amount, Long bidderId, Long bidId) {
        Objects.requireNonNull(amount, "최고입찰 금액은 null일 수 없습니다.");
        Objects.requireNonNull(bidderId, "최고입찰자는 null일 수 없습니다.");
        Objects.requireNonNull(bidId, "최고입찰 ID는 null일 수 없습니다.");
        return new HighestBid(amount, bidderId, bidId);
    }

    public boolean isBidder(Long memberId) {
        return bidderId.equals(memberId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HighestBid h)) return false;
        return Objects.equals(amount, h.amount)
                && Objects.equals(bidderId, h.bidderId)
                && Objects.equals(bidId, h.bidId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, bidderId, bidId);
    }

}
