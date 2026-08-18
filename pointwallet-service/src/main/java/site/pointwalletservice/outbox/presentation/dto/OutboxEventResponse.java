// outbox/presentation/dto/OutboxEventResponse.java
package site.pointwalletservice.outbox.presentation.dto;
import java.time.LocalDateTime;
import site.pointwalletservice.outbox.domain.OutboxEvent;
import site.pointwalletservice.outbox.domain.OutboxEventStatus;

public record OutboxEventResponse(
        Long id,
        String topic,
        String eventType,
        OutboxEventStatus status,
        int retryCount,
        String lastError,
        LocalDateTime createdAt,
        LocalDateTime publishedAt
) {
    public static OutboxEventResponse from(OutboxEvent event) {
        return new OutboxEventResponse(
                event.getId(), event.getTopic(), event.getEventType(), event.getStatus(),
                event.getRetryCount(), event.getLastError(), event.getCreatedAt(), event.getPublishedAt()
        );
    }
}