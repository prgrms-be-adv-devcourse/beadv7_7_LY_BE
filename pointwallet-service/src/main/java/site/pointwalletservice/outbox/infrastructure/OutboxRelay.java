package site.pointwalletservice.outbox.infrastructure;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import site.pointwalletservice.outbox.domain.OutboxEvent;
import site.pointwalletservice.outbox.domain.OutboxEventRepository;
import site.pointwalletservice.outbox.domain.OutboxEventStatus;
import tools.jackson.databind.json.JsonMapper;

/**
 * relay()는 더 이상 @Transactional이 아니다 - Kafka 발행(블로킹 네트워크 I/O)을 DB 트랜잭션
 * 밖에서 하기 위해서다. 예전엔 최대 200개(PENDING 100 + FAILED 100) 이벤트를 하나의 트랜잭션
 * 안에서 순차적으로 blocking send 했는데, 그러면 (1) Kafka가 느려질수록 DB 커넥션이 그
 * 시간만큼 통째로 물려있고, (2) 배치 중간에 예기치 않은 예외가 나면 이미 발행 성공한 앞쪽
 * 이벤트들의 markPublished()까지 같이 롤백돼버렸다.
 * <p>
 * 지금은 이벤트 목록 조회(Spring Data 리포지토리가 메서드 단위로 자체 read-only 트랜잭션을
 * 열어 처리)와, 발행 결과에 따른 상태 갱신(markResult류 - 이벤트 하나당 짧은 트랜잭션)만
 * 분리했다. Kafka 전송 자체는 어떤 트랜잭션에도 속하지 않는다.
 */
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

    @Scheduled(fixedDelayString = "${outbox.relay.polling-delay-ms:15000}")
    public void relay() {
        List<OutboxEvent> targets = new ArrayList<>();
        // FAILED는 종착 상태가 아니라 재시도 대상이다 - 한도(OutboxEvent.MAX_RETRY_COUNT)를 넘기면
        // markFailed()가 DEAD로 전환하므로, 여기서 다시 집혀도 무한 재시도로 이어지지 않는다.
        targets.addAll(outboxEventRepository.findPendingOldestFirst(BATCH_SIZE));
        targets.addAll(outboxEventRepository.findFailedOldestFirst(BATCH_SIZE));

        for (OutboxEvent target : targets) {
            publishOne(target.getId(), target.getTopic(), target.getPartitionKey(),
                    target.getEventType(), target.getPayload());
        }
    }

    /** Kafka 발행(트랜잭션 밖) → 결과에 따라 markPublished/markFailed(각각 짧은 트랜잭션). */
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
            // 이 이벤트는 saga에 묶여 있지 않아 실패해도 아무도 자동으로 감지/보상하지 않는다.
            // DEAD로 넘어가는 순간(=더 이상 재시도 안 됨)만큼은 반드시 사람이 봐야 하므로 메트릭으로 남긴다.
            // Prometheus alert 예: increase(outbox_event_dead_total[1h]) > 0
            if (outboxEvent.getStatus() == OutboxEventStatus.DEAD) {
                meterRegistry.counter("outbox.event.dead", "eventType", eventType).increment();
            }
        });
    }
}