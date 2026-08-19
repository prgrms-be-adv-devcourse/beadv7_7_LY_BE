package site.common.event.contract;

import lombok.Getter;

@Getter
public enum EventType {
    // Auction
    AUCTION_WON_EVENT("auction.won"),
    AUCTION_FORCE_CANCELED_EVENT("auction.force_canceled"),

    // Order
    ORDER_CANCELLED_EVENT("order.cancelled"),
    ORDER_COMPLETED_EVENT("order.completed"),
    ORDER_REFUNDED_EVENT("order.refunded"),

    // Settlement
    SETTLEMENT_CONFIRMED_EVENT("settlement.confirmed"),

    // Wishlist
    WISHLIST_CHANGED_EVENT("wishlist.changed"),

    ;

    private final String value;

    EventType(final String value) {
        this.value = value;
    }
}
