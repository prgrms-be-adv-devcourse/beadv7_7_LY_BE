// outbox/infrastructure/OutboxRelay.java (import·필드 타입만 교체, 나머지 로직 동일)
package site.pointwalletservice.outbox.infrastructure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.pointwalletservice.outbox.domain.OutboxEvent;
import site.pointwalletservice.outbox.domain.OutboxEventRepository;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private static final int BATCH_SIZE = 100;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final JsonMapper jsonMapper;

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void relay() {
        for (OutboxEvent outboxEvent : outboxEventRepository.findPendingOldestFirst(BATCH_SIZE)) {
            publishOne(outboxEvent);
        }
    }

    private void publishOne(OutboxEvent outboxEvent) {
        try {
            Object event = jsonMapper.readValue(
                    outboxEvent.getPayload(), Class.forName(outboxEvent.getEventType()));
            kafkaTemplate.send(outboxEvent.getTopic(), outboxEvent.getPartitionKey(), event).get();
            outboxEvent.markPublished();
        } catch (Exception e) {
            log.error("Outbox 이벤트 발행 실패 — id={}, eventType={}",
                    outboxEvent.getId(), outboxEvent.getEventType(), e);
            outboxEvent.markFailed(e.getMessage());
        }
    }
}