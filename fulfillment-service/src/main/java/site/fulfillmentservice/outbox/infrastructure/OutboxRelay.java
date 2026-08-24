package site.fulfillmentservice.outbox.infrastructure;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import site.fulfillmentservice.outbox.domain.OutboxEvent;
import site.fulfillmentservice.outbox.domain.OutboxEventRepository;
import site.fulfillmentservice.outbox.domain.OutboxEventStatus;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private static final int BATCH_SIZE = 100;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final JsonMapper jsonMapper;
    private final MeterRegistry meterRegistry;
    private final TransactionTemplate transactionTemplate;

    @Scheduled(scheduler = "outboxRelayScheduler", fixedDelayString = "${outbox.relay.polling-delay-ms:15000}")
    public void relay() {
        List<OutboxEvent> targets = new ArrayList<>();
        targets.addAll(outboxEventRepository.findPendingOldestFirst(BATCH_SIZE));
        targets.addAll(outboxEventRepository.findFailedOldestFirst(BATCH_SIZE));

        for (OutboxEvent target : targets) {
            publishOne(target.getId(), target.getTopic(), target.getPartitionKey(),
                target.getEventType(), target.getPayload());
        }
    }

    private void publishOne(Long id, String topic, String partitionKey, String eventType, String payload) {
        try {
            Object event = jsonMapper.readValue(payload, Class.forName(eventType));
            kafkaTemplate.send(topic, partitionKey, event).get();
            markPublished(id);
        } catch (Exception e) {
            log.error("Outbox 이벤트 발행 실패 — id={}, eventType={}", id, eventType, e);
            markFailed(id, eventType, e.getMessage());
        }
    }

    private void markPublished(Long id) {
        transactionTemplate.executeWithoutResult(status -> {
            OutboxEvent outboxEvent = outboxEventRepository.findById(id).orElse(null);
            if (outboxEvent == null) {
                log.warn("상태 갱신 대상 outbox 이벤트를 찾을 수 없음 — id={}", id);
                return;
            }
            outboxEvent.markPublished();
            outboxEventRepository.save(outboxEvent);
        });
    }

    private void markFailed(Long id, String eventType, String reason) {
        transactionTemplate.executeWithoutResult(status -> {
            OutboxEvent outboxEvent = outboxEventRepository.findById(id).orElse(null);
            if (outboxEvent == null) {
                log.warn("상태 갱신 대상 outbox 이벤트를 찾을 수 없음 — id={}", id);
                return;
            }
            outboxEvent.markFailed(reason);
            outboxEventRepository.save(outboxEvent);
            if (outboxEvent.getStatus() == OutboxEventStatus.DEAD) {
                meterRegistry.counter("outbox.event.dead", "eventType", eventType).increment();
            }
        });
    }
}
