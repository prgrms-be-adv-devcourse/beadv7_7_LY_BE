package site.fulfillmentservice.outbox.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("OutboxEvent")
class OutboxEventTest {

    private static OutboxEvent create() {
        return OutboxEvent.create(
                "order.completed", "1001", "site.common.event.contract.OrderCompletedEvent", "{}");
    }

    @Nested
    @DisplayName("생성")
    class Creation {

        @Test
        @DisplayName("생성 시 전달한 값들이 그대로 저장된다")
        void create_전달한_값들이_그대로_저장된다() {
            // when
            OutboxEvent event = create();

            // then
            assertThat(event.getTopic()).isEqualTo("order.completed");
            assertThat(event.getPartitionKey()).isEqualTo("1001");
            assertThat(event.getEventType()).isEqualTo("site.common.event.contract.OrderCompletedEvent");
            assertThat(event.getPayload()).isEqualTo("{}");
        }

        @Test
        @DisplayName("생성 시 PENDING 상태이고 retryCount는 0이다")
        void create_초기상태는_PENDING이고_retryCount는_0이다() {
            // when
            OutboxEvent event = create();

            // then
            assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
            assertThat(event.getRetryCount()).isEqualTo(0);
            assertThat(event.getCreatedAt()).isNotNull();
            assertThat(event.getPublishedAt()).isNull();
            assertThat(event.getLastError()).isNull();
        }
    }

    @Nested
    @DisplayName("markPublished")
    class MarkPublished {

        @Test
        @DisplayName("호출하면 PUBLISHED 상태로 전환되고 publishedAt이 기록된다")
        void markPublished_PUBLISHED로_전환되고_publishedAt이_기록된다() {
            // given
            OutboxEvent event = create();

            // when
            event.markPublished();

            // then
            assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
            assertThat(event.getPublishedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("markFailed")
    class MarkFailed {

        @Test
        @DisplayName("MAX_RETRY_COUNT 미만이면 FAILED 상태를 유지하고 retryCount와 lastError가 갱신된다")
        void markFailed_한도_미만이면_FAILED를_유지한다() {
            // given
            OutboxEvent event = create();

            // when
            event.markFailed("Kafka 연결 실패");

            // then
            assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
            assertThat(event.getRetryCount()).isEqualTo(1);
            assertThat(event.getLastError()).isEqualTo("Kafka 연결 실패");
        }

        @Test
        @DisplayName("MAX_RETRY_COUNT보다 1번 적게 실패하면 여전히 FAILED를 유지한다")
        void markFailed_한도_직전이면_FAILED를_유지한다() {
            // given
            OutboxEvent event = create();

            // when
            for (int i = 0; i < OutboxEvent.MAX_RETRY_COUNT - 1; i++) {
                event.markFailed("실패 " + i);
            }

            // then
            assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
            assertThat(event.getRetryCount()).isEqualTo(OutboxEvent.MAX_RETRY_COUNT - 1);
        }

        @Test
        @DisplayName("MAX_RETRY_COUNT만큼 실패하면 DEAD로 전환된다")
        void markFailed_한도를_넘기면_DEAD로_전환된다() {
            // given
            OutboxEvent event = create();

            // when
            for (int i = 0; i < OutboxEvent.MAX_RETRY_COUNT; i++) {
                event.markFailed("실패 " + i);
            }

            // then
            assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.DEAD);
            assertThat(event.getRetryCount()).isEqualTo(OutboxEvent.MAX_RETRY_COUNT);
        }

        @Test
        @DisplayName("실패마다 lastError가 가장 최근 사유로 덮어써진다")
        void markFailed_lastError는_최근_실패_사유로_덮어써진다() {
            // given
            OutboxEvent event = create();

            // when
            event.markFailed("첫 번째 실패");
            event.markFailed("두 번째 실패");

            // then
            assertThat(event.getLastError()).isEqualTo("두 번째 실패");
        }
    }
}
