package site.fulfillmentservice.outbox.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import site.fulfillmentservice.outbox.domain.OutboxEvent;
import site.fulfillmentservice.outbox.domain.OutboxEventRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class OutboxEventStore {

    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper jsonMapper;

    public void store(String topic, String partitionKey, Object event) {
        String payload;
        try {
            payload = jsonMapper.writeValueAsString(event);
        } catch (JacksonException e) {
            throw new IllegalStateException("Outbox 이벤트 직렬화 실패: " + event.getClass().getName(), e);
        }
        outboxEventRepository.save(
            OutboxEvent.create(topic, partitionKey, event.getClass().getName(), payload)
        );
    }
}
