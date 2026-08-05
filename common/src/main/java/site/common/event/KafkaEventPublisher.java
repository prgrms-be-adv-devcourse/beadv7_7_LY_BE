package site.common.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaEventPublisher(final KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(final Event event) {
        kafkaTemplate.send(event.getEventType(), event).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("이벤트 발행 실패 — eventType: {}, eventId: {}",
                    event.getEventType(), event.getEventId(), ex);
            }
        });
    }
}
