package site.pointwalletservice.outbox;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;
import java.math.BigDecimal;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import site.pointwalletservice.outbox.application.OutboxEventStore;
import site.pointwalletservice.outbox.infrastructure.OutboxRelay;
import site.pointwalletservice.wallet.application.WithdrawFeeEarnedEventHandler;
import site.pointwalletservice.withdraw.domain.event.WithdrawFeeEarnedEvent;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "member.service.base-url=http://localhost:0",
                "toss.payments.client-key=test-client-key",
                "toss.payments.secret-key=test-secret-key"
        }
)
@EmbeddedKafka(partitions = 1, topics = {WithdrawFeeEarnedEvent.TOPIC})
@ActiveProfiles("test")
@DisplayName("Outbox → Kafka 발행/소비 직렬화 왕복 (임베디드 브로커)")
class OutboxKafkaSerializationIntegrationTest {

    @Autowired
    private OutboxEventStore outboxEventStore;

    @Autowired
    private OutboxRelay outboxRelay;

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @MockitoBean
    private WithdrawFeeEarnedEventHandler withdrawFeeEarnedEventHandler;

    @Test
    @DisplayName("Outbox에 저장한 이벤트가 별도 타입 매핑 설정 없이도 필드 값까지 그대로 컨슈머에 도달한다")
    void 직렬화_설정_없이도_필드값까지_정확하게_왕복된다() {
        // given: 컨슈머가 파티션 할당을 마칠 때까지 먼저 기다린다 - 안 그러면
        // auto-offset-reset 기본값(latest) 때문에 지금 보낼 메시지를 놓칠 수 있다.
        MessageListenerContainer container = registry.getListenerContainer("withdrawFeeEarnedEventListener");
        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());

        Long withdrawId = 42L;
        BigDecimal feeAmount = new BigDecimal("2000");
        WithdrawFeeEarnedEvent original = new WithdrawFeeEarnedEvent(withdrawId, feeAmount);

        outboxEventStore.store(WithdrawFeeEarnedEvent.TOPIC, withdrawId.toString(), original);

        // when
        outboxRelay.relay();

        // then
        ArgumentCaptor<WithdrawFeeEarnedEvent> captor = ArgumentCaptor.forClass(WithdrawFeeEarnedEvent.class);
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> verify(withdrawFeeEarnedEventHandler).handle(captor.capture()));

        WithdrawFeeEarnedEvent received = captor.getValue();
        assertThat(received.withdrawId()).isEqualTo(withdrawId);
        assertThat(received.feeAmount()).isEqualByComparingTo(feeAmount);
    }
}