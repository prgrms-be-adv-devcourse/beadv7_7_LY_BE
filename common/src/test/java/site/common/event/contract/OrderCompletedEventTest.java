package site.common.event.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Event의 eventId/occurredAt은 JSON 왕복(발행자 → 컨슈머) 시 원래 값이 그대로 보존돼야 eventId 기준 멱등성 처리가 가능하다. 빌더 생성자에
 * eventId/occurredAt을 추가하기 전에는 Event의 no-arg 슈퍼 생성자가 역직렬화 때도 매번 새로 채번해서, 발행자가 보낸 eventId가 컨슈머에 도착하면
 * 다른 값으로 바뀌어 있었다.
 */
class OrderCompletedEventTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    @DisplayName("JSON으로 내보냈다가 다시 읽어도 eventId/occurredAt이 그대로 보존된다")
    void 왕복_시_eventId_occurredAt_보존() {
        // given
        OrderCompletedEvent original = OrderCompletedEvent.builder()
            .orderId(1L)
            .auctionId(2L)
            .buyerId(3L)
            .sellerId(4L)
            .finalBidPrice(BigDecimal.valueOf(15_000))
            .completedAt(LocalDateTime.of(2026, 7, 27, 10, 0))
            .build();

        // when
        String json = objectMapper.writeValueAsString(original);
        OrderCompletedEvent restored = objectMapper.readValue(json, OrderCompletedEvent.class);

        // then
        assertThat(restored.getEventId()).isEqualTo(original.getEventId());
        assertThat(restored.getOccurredAt()).isEqualTo(original.getOccurredAt());
        assertThat(restored.getOrderId()).isEqualTo(1L);
        assertThat(restored.getAuctionId()).isEqualTo(2L);
        assertThat(restored.getFinalBidPrice()).isEqualByComparingTo(BigDecimal.valueOf(15_000));
    }

    @Test
    @DisplayName("빌더에서 eventId를 지정하지 않으면 새로 채번된다")
    void eventId_미지정시_자동_채번() {
        // given-when
        OrderCompletedEvent first = OrderCompletedEvent.builder().orderId(1L).build();
        OrderCompletedEvent second = OrderCompletedEvent.builder().orderId(1L).build();

        // then
        assertThat(first.getEventId()).isNotNull();
        assertThat(first.getEventId()).isNotEqualTo(second.getEventId());
    }
}
