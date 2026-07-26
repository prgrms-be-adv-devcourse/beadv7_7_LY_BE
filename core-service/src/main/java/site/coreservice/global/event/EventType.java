package site.coreservice.global.event;

import lombok.Getter;

@Getter
public enum EventType {
    AUCTION_WON_EVENT("auction.won"),
    ORDER_CANCELLED_EVENT("order.cancelled"),
    ORDER_COMPLETED_EVENT("order.completed"),

    ;

    private final String value;

    EventType(final String value) {
        this.value = value;
    }
}
