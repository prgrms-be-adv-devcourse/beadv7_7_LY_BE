package site.pointwalletservice.outbox.infrastructure;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.pointwalletservice.outbox.domain.OutboxEvent;
import site.pointwalletservice.outbox.domain.OutboxEventRepository;
import site.pointwalletservice.outbox.domain.OutboxEventStatus;
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

    // 인출은 정산 배치-이벤트 기반이라 발행 지연에 크게 민감하지 않고, 하루 발생량 자체가 적어
    // DB 커넥션 풀(전체 한도 150) 여유를 위해 3초보다 여유 있게 잡았다. 값은 outbox.relay.polling-delay-ms로
    // 재배포 없이 조절 가능 - 인출량이 늘어나 지연이 체감되면 이 값을 낮추면 된다.
    @Scheduled(fixedDelayString = "${outbox.relay.polling-delay-ms:15000}")
    @Transactional
    public void relay() {
        for (OutboxEvent outboxEvent : outboxEventRepository.findPendingOldestFirst(BATCH_SIZE)) {
            publishOne(outboxEvent);
        }
        // FAILED는 종착 상태가 아니라 재시도 대상이다 - 한도(OutboxEvent.MAX_RETRY_COUNT)를 넘기면
        // markFailed()가 DEAD로 전환하므로, 여기서 다시 집혀도 무한 재시도로 이어지지 않는다.
        for (OutboxEvent outboxEvent : outboxEventRepository.findFailedOldestFirst(BATCH_SIZE)) {
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
            // 이 이벤트는 saga에 묶여 있지 않아 실패해도 아무도 자동으로 감지/보상하지 않는다.
            // DEAD로 넘어가는 순간(=더 이상 재시도 안 됨)만큼은 반드시 사람이 봐야 하므로 메트릭으로 남긴다.
            // Prometheus alert 예: increase(outbox_event_dead_total[1h]) > 0
            if (outboxEvent.getStatus() == OutboxEventStatus.DEAD) {
                meterRegistry.counter("outbox.event.dead", "eventType", outboxEvent.getEventType()).increment();
            }
        }
    }
}