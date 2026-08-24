package site.fulfillmentservice.outbox.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.common.event.contract.OrderCancelledEvent;
import site.fulfillmentservice.outbox.domain.OutboxEvent;
import site.fulfillmentservice.outbox.domain.OutboxEventRepository;
import site.fulfillmentservice.outbox.domain.OutboxEventStatus;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxEventStore")
class OutboxEventStoreTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private OutboxEventStore sut;

    @BeforeEach
    void setUp() {
        sut = new OutboxEventStore(outboxEventRepository, JsonMapper.builder().build());
    }

    private static class BrokenEvent {
        public String getValue() {
            throw new IllegalArgumentException("getter boom");
        }
    }

    @Nested
    @DisplayName("직렬화 성공")
    class SerializationSuccess {

        @Test
        @DisplayName("이벤트를 JSON으로 직렬화해 PENDING 상태의 OutboxEvent로 저장한다")
        void store_이벤트를_직렬화해_저장한다() {
            // given
            OrderCancelledEvent event = OrderCancelledEvent.builder()
                    .orderId(1001L)
                    .auctionId(5001L)
                    .buyerId(301L)
                    .build();

            // when
            sut.store("order.cancelled", "1001", event);

            // then
            ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
            verify(outboxEventRepository).save(captor.capture());

            OutboxEvent saved = captor.getValue();
            assertThat(saved.getTopic()).isEqualTo("order.cancelled");
            assertThat(saved.getPartitionKey()).isEqualTo("1001");
            assertThat(saved.getEventType()).isEqualTo(OrderCancelledEvent.class.getName());
            assertThat(saved.getPayload()).contains("\"orderId\":1001");
            assertThat(saved.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("직렬화 실패")
    class SerializationFailure {

        @Test
        @DisplayName("직렬화에 실패하면 IllegalStateException을 던지고 저장하지 않는다")
        void store_직렬화_실패하면_예외를_던지고_저장하지_않는다() {
            // given
            BrokenEvent event = new BrokenEvent();

            // when & then
            assertThatThrownBy(() -> sut.store("order.cancelled", "1001", event))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Outbox 이벤트 직렬화 실패");

            verifyNoInteractions(outboxEventRepository);
        }
    }
}
