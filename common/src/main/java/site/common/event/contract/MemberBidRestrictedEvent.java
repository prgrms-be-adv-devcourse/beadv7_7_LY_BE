package site.common.event.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import site.common.event.Event;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class MemberBidRestrictedEvent extends Event {

    private final Long memberId;

    @Builder
    @JsonCreator
    private MemberBidRestrictedEvent(
        @JsonProperty("eventId") final UUID eventId,
        @JsonProperty("occurredAt") final LocalDateTime occurredAt,
        @JsonProperty("memberId") final Long memberId
    ) {
        super(eventId, occurredAt);
        this.memberId = memberId;
    }

    @Override
    public String getEventType() {
        return EventType.MEMBER_BID_RESTRICTED_EVENT.getValue();
    }
}
