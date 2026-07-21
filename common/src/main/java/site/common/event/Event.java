package site.common.event;

import java.time.LocalDateTime;
import lombok.Getter;

import java.util.UUID;

@Getter
public abstract class Event {

    private final UUID eventId;
    private final LocalDateTime occurredAt;

    protected Event() {
        this.eventId = UUID.randomUUID();
        this.occurredAt = LocalDateTime.now();
    }

    public abstract String getEventType();
}
