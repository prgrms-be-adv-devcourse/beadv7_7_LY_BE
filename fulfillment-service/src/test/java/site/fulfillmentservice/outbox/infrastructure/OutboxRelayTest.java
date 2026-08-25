package site.fulfillmentservice.outbox.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import site.common.event.contract.EventType;
import site.common.event.contract.OrderCancelledEvent;
import site.fulfillmentservice.outbox.domain.OutboxEvent;
import site.fulfillmentservice.outbox.domain.OutboxEventRepository;
import site.fulfillmentservice.outbox.domain.OutboxEventStatus;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxRelay")
class OutboxRelayTest {

    private static final String TOPIC = EventType.ORDER_CANCELLED_EVENT.getValue();

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private TransactionTemplate transactionTemplate;

    private JsonMapper jsonMapper;
    private SimpleMeterRegistry meterRegistry;
    private OutboxRelay sut;

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapper.builder().build();
        meterRegistry = new SimpleMeterRegistry();
        sut = new OutboxRelay(outboxEventRepository, kafkaTemplate, jsonMapper, meterRegistry, transactionTemplate);

        // markPublished/markFailed가 transactionTemplate.executeWithoutResult(...)로 상태를 갱신하므로,
        // 실제 트랜잭션 없이도 콜백이 그 자리에서 실행되도록 스텁한다.
        doAnswer(invocation -> {
            Consumer<TransactionStatus> action = invocation.getArgument(0);
            action.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    private OutboxEvent createWithId(Long id, String topic, String partitionKey, String eventType, String payload) {
        OutboxEvent event = OutboxEvent.create(topic, partitionKey, eventType, payload);
        ReflectionTestUtils.setField(event, "id", id);
        when(outboxEventRepository.findById(id)).thenReturn(Optional.of(event));
        return event;
    }

    private String write(Object event) {
        try {
            return jsonMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("PENDING 행을 원래 이벤트 타입으로 역직렬화해서 발행하고, 성공하면 PUBLISHED 상태로 바뀐다")
    void relay_대기중인_이벤트를_발행하고_상태를_갱신한다() {
        // given
        OrderCancelledEvent original = OrderCancelledEvent.builder()
                .orderId(1001L).auctionId(5001L).buyerId(301L).build();
        String payload = write(original);
        OutboxEvent pending = createWithId(1L, TOPIC, "1001", OrderCancelledEvent.class.getName(), payload);
        when(outboxEventRepository.findPendingOldestFirst(100)).thenReturn(List.of(pending));
        when(outboxEventRepository.findFailedOldestFirst(100)).thenReturn(List.of());
        when(kafkaTemplate.send(eq(TOPIC), eq("1001"), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        // when
        sut.relay();

        // then
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq(TOPIC), eq("1001"), captor.capture());
        OrderCancelledEvent sent = (OrderCancelledEvent) captor.getValue();
        assertThat(sent.getOrderId()).isEqualTo(1001L);
        verify(outboxEventRepository).save(pending);
        assertThat(pending.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThat(pending.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("발행이 실패하면(예: 역직렬화 불가) FAILED로 표시하고, 다음 행 처리는 계속한다")
    void relay_발행실패시_FAILED로_표시하고_나머지는_계속처리한다() {
        // given: eventType을 존재하지 않는 클래스명으로 만들어 역직렬화 자체가 실패하게 함
        OutboxEvent broken = createWithId(1L, TOPIC, "1", "no.such.EventClass", "{}");
        String validPayload = write(
                OrderCancelledEvent.builder().orderId(1002L).auctionId(5002L).buyerId(302L).build());
        OutboxEvent valid = createWithId(2L, TOPIC, "1002", OrderCancelledEvent.class.getName(), validPayload);
        when(outboxEventRepository.findPendingOldestFirst(100)).thenReturn(List.of(broken, valid));
        when(outboxEventRepository.findFailedOldestFirst(100)).thenReturn(List.of());
        when(kafkaTemplate.send(eq(TOPIC), eq("1002"), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        // when
        sut.relay();

        // then
        assertThat(broken.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(broken.getLastError()).isNotNull();
        assertThat(valid.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        verify(kafkaTemplate, times(1)).send(eq(TOPIC), eq("1002"), any());
    }

    @Test
    @DisplayName("역직렬화는 성공했지만 Kafka 발행(send) 자체가 실패하면 FAILED로 표시된다")
    void relay_Kafka_발행이_실패하면_FAILED로_표시한다() {
        // given
        String payload = write(
                OrderCancelledEvent.builder().orderId(1006L).auctionId(5006L).buyerId(306L).build());
        OutboxEvent pending = createWithId(6L, TOPIC, "1006", OrderCancelledEvent.class.getName(), payload);
        when(outboxEventRepository.findPendingOldestFirst(100)).thenReturn(List.of(pending));
        when(outboxEventRepository.findFailedOldestFirst(100)).thenReturn(List.of());
        when(kafkaTemplate.send(eq(TOPIC), eq("1006"), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka 연결 실패")));

        // when
        sut.relay();

        // then
        assertThat(pending.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(pending.getRetryCount()).isEqualTo(1);
        assertThat(pending.getLastError()).isNotNull();
    }

    @Test
    @DisplayName("PENDING과 함께 FAILED 행도 재시도 대상으로 폴링하고, 발행에 성공하면 PUBLISHED로 바뀐다")
    void relay_FAILED_행도_재시도해서_발행한다() {
        // given
        String payload = write(
                OrderCancelledEvent.builder().orderId(1003L).auctionId(5003L).buyerId(303L).build());
        OutboxEvent failed = createWithId(3L, TOPIC, "1003", OrderCancelledEvent.class.getName(), payload);
        failed.markFailed("일시적인 카프카 전송 실패");
        when(outboxEventRepository.findPendingOldestFirst(100)).thenReturn(List.of());
        when(outboxEventRepository.findFailedOldestFirst(100)).thenReturn(List.of(failed));
        when(kafkaTemplate.send(eq(TOPIC), eq("1003"), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        // when
        sut.relay();

        // then
        assertThat(failed.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        verify(kafkaTemplate, times(1)).send(eq(TOPIC), eq("1003"), any());
    }

    @Test
    @DisplayName("재시도 한도(MAX_RETRY_COUNT)를 넘겨 계속 실패하면 FAILED가 아니라 DEAD로 전환되고 메트릭이 증가한다")
    void relay_재시도_한도를_넘기면_DEAD로_전환된다() {
        // given: 이미 MAX_RETRY_COUNT - 1번 실패한 상태에서 이번에도 실패
        OutboxEvent broken = createWithId(4L, TOPIC, "1004", "no.such.EventClass", "{}");
        for (int i = 0; i < OutboxEvent.MAX_RETRY_COUNT - 1; i++) {
            broken.markFailed("이전 실패");
        }
        when(outboxEventRepository.findPendingOldestFirst(100)).thenReturn(List.of());
        when(outboxEventRepository.findFailedOldestFirst(100)).thenReturn(List.of(broken));

        // when
        sut.relay();

        // then
        assertThat(broken.getStatus()).isEqualTo(OutboxEventStatus.DEAD);
        assertThat(meterRegistry.counter("outbox.event.dead", "eventType", "no.such.EventClass").count())
                .isEqualTo(1.0);
    }

    @Test
    @DisplayName("상태 갱신 시점에 이미 삭제된 행이면 예외 없이 건너뛴다")
    void relay_상태갱신_대상_행이_없으면_예외없이_건너뛴다() {
        // given: 목록 조회 시점엔 있었지만 markPublished 시점(findById)엔 없다고 가정
        String payload = write(
                OrderCancelledEvent.builder().orderId(1005L).auctionId(5005L).buyerId(305L).build());
        OutboxEvent pending = OutboxEvent.create(TOPIC, "1005", OrderCancelledEvent.class.getName(), payload);
        ReflectionTestUtils.setField(pending, "id", 5L);
        when(outboxEventRepository.findById(5L)).thenReturn(Optional.empty());
        when(outboxEventRepository.findPendingOldestFirst(100)).thenReturn(List.of(pending));
        when(outboxEventRepository.findFailedOldestFirst(100)).thenReturn(List.of());
        when(kafkaTemplate.send(eq(TOPIC), eq("1005"), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        // when / then — 예외를 던지지 않고 조용히 넘어가야 한다
        sut.relay();
        verify(outboxEventRepository, times(0)).save(any());
    }
}
