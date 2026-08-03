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

    protected Event() {
        this.eventId = UUID.randomUUID();
        this.occurredAt = LocalDateTime.now();
    }

    public abstract String getEventType();
}
