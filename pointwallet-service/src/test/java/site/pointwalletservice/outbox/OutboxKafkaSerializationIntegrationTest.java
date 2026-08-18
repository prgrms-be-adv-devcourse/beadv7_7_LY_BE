package site.pointwalletservice.outbox;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;
import java.math.BigDecimal;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.mockito.ArgumentCaptor;
import site.pointwalletservice.outbox.application.OutboxEventStore;
import site.pointwalletservice.outbox.infrastructure.OutboxRelay;
import site.pointwalletservice.wallet.application.WithdrawFeeEarnedEventHandler;
import site.pointwalletservice.withdraw.domain.event.WithdrawFeeEarnedEvent;

/**
 * Outbox 저장(JsonMapper로 직렬화) → OutboxRelay 발행(같은 JsonMapper로 역직렬화 후
 * JacksonJsonSerializer로 재직렬화) → 실제 브로커 전송 → 컨슈머(ErrorHandlingDeserializer +
 * JacksonJsonDeserializer, trusted.packages 화이트리스트) → @KafkaListener 순서로
 * "정말 별도 타입 매핑 설정 없이도 왕복이 되는지"를 실제 임베디드 브로커로 검증한다.
 * <p>
 * 이 체인은 서로 다른 두 군데(수동 주입 JsonMapper / Kafka 자동설정 JacksonJsonSerializer)가
 * 같은 방식으로 동작한다는 가정 위에 서있어서, 코드 리뷰만으로는 "될 것 같다"까지만 말할 수 있고
 * 실제로 브로커를 띄워봐야 확신할 수 있다 - WithdrawFeeEarnedEventHandler만 목으로 바꿔서
 * "최종적으로 필드 값까지 정확한 WithdrawFeeEarnedEvent가 컨슈머에 도달하는지"를 확인한다.
 */
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

    @MockitoBean
    private WithdrawFeeEarnedEventHandler withdrawFeeEarnedEventHandler;

    @Test
    @DisplayName("Outbox에 저장한 이벤트가 별도 타입 매핑 설정 없이도 필드 값까지 그대로 컨슈머에 도달한다")
    void 직렬화_설정_없이도_필드값까지_정확하게_왕복된다() {
        // given
        Long withdrawId = 42L;
        BigDecimal feeAmount = new BigDecimal("2000");
        WithdrawFeeEarnedEvent original = new WithdrawFeeEarnedEvent(withdrawId, feeAmount);

        outboxEventStore.store(WithdrawFeeEarnedEvent.TOPIC, withdrawId.toString(), original);

        // when: @Scheduled는 테스트에서 안 도니까 직접 호출해서 실제 브로커로 발행시킨다
        outboxRelay.relay();

        // then: 실제 컨슈머(@KafkaListener)가 브로커에서 받아 역직렬화한 뒤 핸들러를 호출할 때까지 대기
        ArgumentCaptor<WithdrawFeeEarnedEvent> captor = ArgumentCaptor.forClass(WithdrawFeeEarnedEvent.class);
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> verify(withdrawFeeEarnedEventHandler).handle(captor.capture()));

        // then: __TypeId__ 헤더 기반 역직렬화가 정확한 타입 + 값으로 이어졌는지 (특히 BigDecimal)
        WithdrawFeeEarnedEvent received = captor.getValue();
        assertThat(received.withdrawId()).isEqualTo(withdrawId);
        assertThat(received.feeAmount()).isEqualByComparingTo(feeAmount);
    }
}