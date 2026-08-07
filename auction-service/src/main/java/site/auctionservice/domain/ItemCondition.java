package site.auctionservice.domain;

import lombok.Getter;
import site.auctionservice.exception.AuctionErrorCode;
import site.auctionservice.exception.AuctionException;

@Getter
public enum ItemCondition {
    MINT("새 제품"),
    NEAR_MINT("거의 새것"),
    VERY_GOOD_PLUS("약간의 사용감"),
    VERY_GOOD("사용감 있음"),
    GOOD("많은 사용감"),
    POOR("심한 손상");

    private final String description;

    ItemCondition(String description) {
        this.description = description;
    }

    public static ItemCondition from(String value) {
        try {
            return ItemCondition.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new AuctionException(AuctionErrorCode.ITEM_CONDITION_INVALID);
        }
    }
}
