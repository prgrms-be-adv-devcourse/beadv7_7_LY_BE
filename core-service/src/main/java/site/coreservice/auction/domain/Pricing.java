package site.coreservice.auction.domain;

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
public class Pricing {

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "start_price", nullable = false))
    private Money startPrice;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "bid_unit", nullable = false))
    private Money bidUnit;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "shipping_fee", nullable = false))
    private Money shippingFee;

    private Pricing(Money startPrice, Money bidUnit, Money shippingFee) {
        this.startPrice = startPrice;
        this.bidUnit = bidUnit;
        this.shippingFee = shippingFee;
    }

    public static Pricing of(Money startPrice, Money bidUnit, Money shippingFee) {
        Objects.requireNonNull(startPrice, "시작가는 null일 수 없습니다.");
        Objects.requireNonNull(bidUnit, "입찰 단위는 null일 수 없습니다.");
        Objects.requireNonNull(shippingFee, "배송비는 null일 수 없습니다.");
        if (startPrice.isLessThan(AuctionPolicy.MIN_START_PRICE)) {
            throw new IllegalArgumentException(
                "시작가는 %s원 이상이어야 합니다.".formatted(AuctionPolicy.MIN_START_PRICE.getValue()));
        }
        if (bidUnit.isLessThan(AuctionPolicy.MIN_BID_UNIT)) {
            throw new IllegalArgumentException(
                "입찰 단위는 %s원 이상이어야 합니다.".formatted(AuctionPolicy.MIN_BID_UNIT.getValue()));
        }
        return new Pricing(startPrice, bidUnit, shippingFee);
    }

    public Money startBidAmount() {
        return startPrice.plus(shippingFee);
    }

    public Money nextMinBidAmount(HighestBid highestBid) {
        return (highestBid == null) ? startBidAmount() : highestBid.getAmount().plus(bidUnit);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Pricing p)) {
            return false;
        }
        return Objects.equals(startPrice, p.startPrice)
            && Objects.equals(bidUnit, p.bidUnit)
            && Objects.equals(shippingFee, p.shippingFee);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startPrice, bidUnit, shippingFee);
    }

}
