package site.fulfillmentservice.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import site.common.event.contract.EventType;
import site.common.event.contract.OrderCompletedEvent;
import site.fulfillmentservice.outbox.application.OutboxEventStore;
import site.fulfillmentservice.outbox.infrastructure.OutboxEventJpaRepository;
import site.fulfillmentservice.outbox.infrastructure.OutboxRelay;
import site.fulfillmentservice.settlement.application.OrderCompletedEventHandler;

/**
 * @Tag("integration") — 로컬 MySQL/Kafka가 떠 있어야 돈다. OutboxEventStore/OutboxRelay가 실제로
 * 주입받는 JsonMapper 빈과, 실제 Kafka value (de)serializer(JacksonJsonSerializer/Deserializer)까지
 * 전부 실제 설정 그대로 거쳐서 필드값이 안 깨지고 도착하는지가 관심사다 — 단위 테스트는 이 두 (de)serializer가
 * 실제로 같은 값을 만들어내는지는 못 본다.
 * <p>
 * 정산 로직(CommissionPolicy 조회 등)까지 실행되게 두면 사전 데이터 세팅이 필요해지므로,
 * OrderCompletedEventHandler를 목으로 끊어 "이벤트가 정확한 값으로 도착했는가"만 본다.
 */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("local")
class OutboxKafkaRoundTripIntegrationTest {

    private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(10);
    private static final Long TEST_ORDER_ID = 900_001L;

    @Autowired
    private OutboxEventStore outboxEventStore;

    @Autowired
    private OutboxRelay outboxRelay;

    @Autowired
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @MockitoBean
    private OrderCompletedEventHandler orderCompletedEventHandler;

    @AfterEach
    void tearDown() {
        outboxEventJpaRepository.findAll().stream()
            .filter(event -> TEST_ORDER_ID.toString().equals(event.getPartitionKey()))
            .forEach(outboxEventJpaRepository::delete);
    }

    @Test
    @DisplayName("outbox에 저장한 이벤트가 실제 Kafka를 거쳐 필드값까지 그대로 컨슈머에 도달한다")
    void outbox_저장부터_Kafka_왕복까지_필드값이_보존된다() {
        // given
        OrderCompletedEvent original = OrderCompletedEvent.builder()
            .orderId(TEST_ORDER_ID)
            .auctionId(5001L)
            .buyerId(301L)
            .sellerId(302L)
            .finalBidPrice(BigDecimal.valueOf(85_000))
            .completedAt(LocalDateTime.of(2026, 7, 22, 14, 30))
            .build();

        outboxEventStore.store(
            EventType.ORDER_COMPLETED_EVENT.getValue(), TEST_ORDER_ID.toString(), original);

        // when
        outboxRelay.relay();

        // then
        ArgumentCaptor<OrderCompletedEvent> captor = ArgumentCaptor.forClass(OrderCompletedEvent.class);
        await().atMost(AWAIT_TIMEOUT).untilAsserted(() ->
            verify(orderCompletedEventHandler).handle(captor.capture()));

        OrderCompletedEvent received = captor.getValue();
        assertThat(received.getOrderId()).isEqualTo(TEST_ORDER_ID);
        assertThat(received.getAuctionId()).isEqualTo(5001L);
        assertThat(received.getBuyerId()).isEqualTo(301L);
        assertThat(received.getSellerId()).isEqualTo(302L);
        assertThat(received.getFinalBidPrice()).isEqualByComparingTo(BigDecimal.valueOf(85_000));
        assertThat(received.getCompletedAt()).isEqualTo(LocalDateTime.of(2026, 7, 22, 14, 30));
    }
}
