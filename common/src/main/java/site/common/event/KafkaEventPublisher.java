package site.common.event;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "event.publisher.type", havingValue = "kafka")
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaEventPublisher(final KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(final Event event) {
        kafkaTemplate.send(event.getEventType(), event);
    }
}
