package site.common.event.contract;

import lombok.Getter;

@Getter
public enum EventType {
    // Auction
    AUCTION_WON_EVENT("auction.won"),

    // Order
    ORDER_CANCELLED_EVENT("order.cancelled"),
    ORDER_COMPLETED_EVENT("order.completed"),

    // Settlement
    SETTLEMENT_CONFIRMED_EVENT("settlement.confirmed"),

    ;

    private final String value;

    EventType(final String value) {
        this.value = value;
    }
}
