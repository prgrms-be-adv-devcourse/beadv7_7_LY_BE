package site.common.event.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import site.common.event.Event;

/**
 * 어떤 회원의 위시리스트가 바뀌었다는 신호. 무엇이 어떻게 바뀌었는지는 담지 않는다.
 */
@Getter
public class WishlistChangedEvent extends Event {

    private final Long memberId;

    @Builder
    @JsonCreator
    private WishlistChangedEvent(
        @JsonProperty("eventId") final UUID eventId,
        @JsonProperty("occurredAt") final LocalDateTime occurredAt,
        @JsonProperty("memberId") final Long memberId
    ) {
        super(eventId, occurredAt);
        this.memberId = memberId;
    }

    // Kafka 토픽
    @Override
    public String getEventType() {
        return EventType.WISHLIST_CHANGED_EVENT.getValue();
    }
}
