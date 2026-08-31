package site.auctionservice.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.auctionservice.exception.AuctionErrorCode;
import site.auctionservice.exception.AuctionException;

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
            throw new AuctionException(AuctionErrorCode.START_PRICE_TOO_LOW);
        }
        if (bidUnit.isLessThan(AuctionPolicy.MIN_BID_UNIT)) {
            throw new AuctionException(AuctionErrorCode.BID_UNIT_TOO_LOW);
        }
        if (!bidUnit.isMultipleOf(AuctionPolicy.MIN_BID_UNIT)) {
            throw new AuctionException(AuctionErrorCode.BID_UNIT_NOT_MULTIPLE_OF_MIN_UNIT);
        }
        if (bidUnit.isGreaterThanOrEqual(startPrice)) {
            throw new AuctionException(AuctionErrorCode.BID_UNIT_NOT_LESS_THAN_START_PRICE);
        }
        return new Pricing(startPrice, bidUnit, shippingFee);
    }

    public Money startBidAmount() {
        return startPrice.plus(shippingFee);
    }

    public Money nextMinBidAmount(HighestBid highestBid) {
        return (highestBid == null) ? startBidAmount() : highestBid.getAmount().plus(bidUnit);
    }

    /* 입찰 금액이 시작 입찰가를 기준으로 입찰 단위의 배수인지 검증 */
    public boolean isAlignedToBidUnit(Money amount) {
        Money startBidAmount = startBidAmount();
        if (amount.isLessThan(startBidAmount)) {
            return false;
        }
        return amount.minus(startBidAmount).isMultipleOf(bidUnit);
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
