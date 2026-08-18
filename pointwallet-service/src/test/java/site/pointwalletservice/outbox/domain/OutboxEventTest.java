package site.pointwalletservice.outbox.domain;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.pointwalletservice.outbox.exception.OutboxException;

@DisplayName("OutboxEvent")
class OutboxEventTest {

    private OutboxEvent createDeadEvent() {
        OutboxEvent event = OutboxEvent.create("topic", "key", "some.EventType", "{}");
        for (int i = 0; i < OutboxEvent.MAX_RETRY_COUNT; i++) {
            event.markFailed("실패 " + i);
        }
        return event;
    }

    @Test
    @DisplayName("MAX_RETRY_COUNT만큼 실패하면 DEAD로 전환된다")
    void markFailed_한도를_넘기면_DEAD로_전환된다() {
        OutboxEvent event = createDeadEvent();

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.DEAD);
        assertThat(event.getRetryCount()).isEqualTo(OutboxEvent.MAX_RETRY_COUNT);
    }

    @Test
    @DisplayName("DEAD 상태에서 retryManually()를 호출하면 PENDING으로 되돌아가고 retryCount가 0으로 리셋된다")
    void retryManually_DEAD상태면_PENDING으로_되돌리고_retryCount를_리셋한다() {
        OutboxEvent event = createDeadEvent();

        event.retryManually();

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(event.getRetryCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("리셋 후 다시 MAX_RETRY_COUNT만큼 실패하면 다시 DEAD로 전환된다 (새로 기회를 받는다)")
    void retryManually_이후_다시_한도를_넘기면_다시_DEAD가_된다() {
        OutboxEvent event = createDeadEvent();
        event.retryManually();

        for (int i = 0; i < OutboxEvent.MAX_RETRY_COUNT; i++) {
            event.markFailed("다시 실패 " + i);
        }

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.DEAD);
        assertThat(event.getRetryCount()).isEqualTo(OutboxEvent.MAX_RETRY_COUNT);
    }

    @Test
    @DisplayName("DEAD가 아닌 상태에서 retryManually()를 호출하면 예외가 발생한다")
    void retryManually_DEAD상태가_아니면_예외() {
        OutboxEvent pending = OutboxEvent.create("topic", "key", "some.EventType", "{}");

        assertThatThrownBy(pending::retryManually)
                .isInstanceOf(OutboxException.class);
    }
}