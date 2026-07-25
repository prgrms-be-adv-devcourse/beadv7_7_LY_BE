package site.coreservice.global.event;

import lombok.Getter;

@Getter
public enum EventType {
    AUCTION_WON_EVENT("auction.won"),

    ;

    private final String value;

    EventType(final String value) {
        this.value = value;
    }
}
