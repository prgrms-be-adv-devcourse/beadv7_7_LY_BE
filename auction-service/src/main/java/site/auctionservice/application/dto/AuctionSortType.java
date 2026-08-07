package site.auctionservice.application.dto;

import site.auctionservice.exception.AuctionErrorCode;
import site.auctionservice.exception.AuctionException;

public enum AuctionSortType {
    ENDING_SOON,
    PRICE_ASC,
    PRICE_DESC,
    MOST_BIDS;

    public static AuctionSortType from(String value) {
        if (value == null) {
            return ENDING_SOON;
        }
        return switch (value) {
            case "ending_soon" -> ENDING_SOON;
            case "price_asc" -> PRICE_ASC;
            case "price_desc" -> PRICE_DESC;
            case "most_bids" -> MOST_BIDS;
            default -> throw new AuctionException(AuctionErrorCode.AUCTION_SORT_INVALID);
        };
    }
}
