package site.common.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import lombok.Getter;

import java.util.UUID;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class Event {

    private final UUID eventId;
    private final LocalDateTime occurredAt;

    // eventId/occurredAt이 null이면(= 새로 만드는 이벤트) 여기서 채번한다. 역직렬화 시점에는
    // 발행자가 보낸 원래 값이 그대로 들어오므로 재채번하지 않는다
    protected Event(final UUID eventId, final LocalDateTime occurredAt) {
        this.eventId = (eventId != null) ? eventId : UUID.randomUUID();
        this.occurredAt = (occurredAt != null) ? occurredAt : LocalDateTime.now();
    }

    public abstract String getEventType();
}
