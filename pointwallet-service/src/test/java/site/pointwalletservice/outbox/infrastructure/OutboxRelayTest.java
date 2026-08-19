package site.pointwalletservice.outbox.infrastructure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import tools.jackson.databind.json.JsonMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import site.pointwalletservice.outbox.domain.OutboxEvent;
import site.pointwalletservice.outbox.domain.OutboxEventRepository;
import site.pointwalletservice.outbox.domain.OutboxEventStatus;
import site.pointwalletservice.withdraw.domain.event.WithdrawFeeEarnedEvent;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxRelay")
class OutboxRelayTest {

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

    /** 테스트용 OutboxEvent를 만들고, findById(id)로 재조회하는 새 로직에 맞춰 id를 심어준다. */
    private OutboxEvent createWithId(Long id, String topic, String partitionKey, String eventType, String payload) {
        OutboxEvent event = OutboxEvent.create(topic, partitionKey, eventType, payload);
        ReflectionTestUtils.setField(event, "id", id);
        when(outboxEventRepository.findById(id)).thenReturn(Optional.of(event));
        return event;
    }

    @Test
    @DisplayName("PENDING 행을 원래 이벤트 타입으로 역직렬화해서 발행하고, 성공하면 markPublished 상태로 남는다")
    void relay_대기중인_이벤트를_발행하고_상태를_갱신한다() throws Exception {
        // given
        String payload = jsonMapper.writeValueAsString(new WithdrawFeeEarnedEvent(1L, java.math.BigDecimal.valueOf(2_000)));
        OutboxEvent pending = createWithId(1L, WithdrawFeeEarnedEvent.TOPIC, "1", WithdrawFeeEarnedEvent.class.getName(), payload);
        when(outboxEventRepository.findPendingOldestFirst(100)).thenReturn(List.of(pending));
        when(outboxEventRepository.findFailedOldestFirst(100)).thenReturn(List.of());
        when(kafkaTemplate.send(eq(WithdrawFeeEarnedEvent.TOPIC), eq("1"), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        // when
        sut.relay();

        // then
        verify(kafkaTemplate).send(eq(WithdrawFeeEarnedEvent.TOPIC), eq("1"),
                eq(new WithdrawFeeEarnedEvent(1L, java.math.BigDecimal.valueOf(2_000))));
        verify(outboxEventRepository).save(pending);
        assertThat(pending.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThat(pending.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("발행이 실패하면(예: 역직렬화 불가) FAILED로 표시하고, 다음 행 처리는 계속한다")
    void relay_발행실패시_FAILED로_표시하고_나머지는_계속처리한다() {
        // given: eventType을 존재하지 않는 클래스명으로 만들어 역직렬화 자체가 실패하게 함
        OutboxEvent broken = createWithId(1L, WithdrawFeeEarnedEvent.TOPIC, "1", "no.such.EventClass", "{}");
        String validPayload = objectMapperWrite(new WithdrawFeeEarnedEvent(2L, java.math.BigDecimal.valueOf(1_000)));
        OutboxEvent valid = createWithId(2L, WithdrawFeeEarnedEvent.TOPIC, "2", WithdrawFeeEarnedEvent.class.getName(), validPayload);
        when(outboxEventRepository.findPendingOldestFirst(100)).thenReturn(List.of(broken, valid));
        when(outboxEventRepository.findFailedOldestFirst(100)).thenReturn(List.of());
        when(kafkaTemplate.send(eq(WithdrawFeeEarnedEvent.TOPIC), eq("2"), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        // when
        sut.relay();

        // then
        assertThat(broken.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(broken.getLastError()).isNotNull();
        assertThat(valid.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        verify(kafkaTemplate, times(1)).send(eq(WithdrawFeeEarnedEvent.TOPIC), eq("2"), any());
    }

    @Test
    @DisplayName("PENDING과 함께 FAILED 행도 재시도 대상으로 폴링하고, 발행에 성공하면 PUBLISHED로 바뀐다")
    void relay_FAILED_행도_재시도해서_발행한다() {
        // given
        String payload = objectMapperWrite(new WithdrawFeeEarnedEvent(3L, java.math.BigDecimal.valueOf(500)));
        OutboxEvent failed = createWithId(3L, WithdrawFeeEarnedEvent.TOPIC, "3", WithdrawFeeEarnedEvent.class.getName(), payload);
        failed.markFailed("일시적인 카프카 전송 실패");
        when(outboxEventRepository.findPendingOldestFirst(100)).thenReturn(List.of());
        when(outboxEventRepository.findFailedOldestFirst(100)).thenReturn(List.of(failed));
        when(kafkaTemplate.send(eq(WithdrawFeeEarnedEvent.TOPIC), eq("3"), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        // when
        sut.relay();

        // then
        assertThat(failed.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        verify(kafkaTemplate, times(1)).send(eq(WithdrawFeeEarnedEvent.TOPIC), eq("3"), any());
    }

    @Test
    @DisplayName("재시도 한도(MAX_RETRY_COUNT)를 넘겨 계속 실패하면 더 이상 FAILED가 아니라 DEAD로 전환된다")
    void relay_재시도_한도를_넘기면_DEAD로_전환된다() {
        // given: 이미 MAX_RETRY_COUNT - 1번 실패한 상태에서 이번에도 실패
        OutboxEvent broken = createWithId(4L, WithdrawFeeEarnedEvent.TOPIC, "4", "no.such.EventClass", "{}");
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
        String payload = objectMapperWrite(new WithdrawFeeEarnedEvent(5L, java.math.BigDecimal.valueOf(100)));
        OutboxEvent pending = OutboxEvent.create(WithdrawFeeEarnedEvent.TOPIC, "5", WithdrawFeeEarnedEvent.class.getName(), payload);
        ReflectionTestUtils.setField(pending, "id", 5L);
        when(outboxEventRepository.findById(5L)).thenReturn(Optional.empty());
        when(outboxEventRepository.findPendingOldestFirst(100)).thenReturn(List.of(pending));
        when(outboxEventRepository.findFailedOldestFirst(100)).thenReturn(List.of());
        when(kafkaTemplate.send(eq(WithdrawFeeEarnedEvent.TOPIC), eq("5"), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        // when / then — 예외를 던지지 않고 조용히 넘어가야 한다
        sut.relay();
        verify(outboxEventRepository, times(0)).save(any());
    }

    private String objectMapperWrite(Object event) {
        try {
            return jsonMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}