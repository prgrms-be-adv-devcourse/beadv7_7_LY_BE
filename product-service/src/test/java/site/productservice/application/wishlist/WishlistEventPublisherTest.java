package site.productservice.application.wishlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.common.event.Event;
import site.common.event.EventPublisher;
import site.common.event.contract.WishlistChangedEvent;

@ExtendWith(MockitoExtension.class)
@DisplayName("WishlistEventPublisher")
class WishlistEventPublisherTest {

    @Mock
    private EventPublisher eventPublisher;

    @Test
    @DisplayName("추가 시 memberId만 담은 이벤트를 발행한다")
    void publishAdded는_memberId만_담는다() {
        final WishlistEventPublisher publisher = new WishlistEventPublisher(eventPublisher);

        publisher.publishAdded(7L);

        final ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventPublisher).publish(captor.capture());

        final WishlistChangedEvent event = (WishlistChangedEvent) captor.getValue();
        assertThat(event.getMemberId()).isEqualTo(7L);
        assertThat(event.getEventType()).isEqualTo("wishlist.changed");
    }

    @Test
    @DisplayName("삭제도 같은 이벤트를 발행한다 — 소비자는 추가·삭제를 구분하지 않고 전체를 다시 계산한다")
    void publishRemoved도_같은_이벤트를_발행한다() {
        final WishlistEventPublisher publisher = new WishlistEventPublisher(eventPublisher);

        publisher.publishRemoved(7L);

        final ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventPublisher).publish(captor.capture());

        assertThat(captor.getValue()).isInstanceOf(WishlistChangedEvent.class);
        assertThat(((WishlistChangedEvent) captor.getValue()).getMemberId()).isEqualTo(7L);
    }

    /**
     * 이 이벤트는 트랜잭션 커밋 이후에 발행되므로, 여기서 예외가 올라가면 위시리스트에는 이미 반영됐는데 사용자만 실패 응답을 받는다. 브로커가 죽어 있으면
     * KafkaTemplate.send()가 동기적으로 예외를 던질 수 있어서(Kafka 전환 때 실제로 겪었다) 실전에서 일어날 수 있는 상황이다.
     */
    @Test
    @DisplayName("발행이 실패해도 예외를 전파하지 않는다")
    void 발행_실패를_삼킨다() {
        willThrow(new RuntimeException("broker down")).given(eventPublisher)
            .publish(org.mockito.ArgumentMatchers.any());
        final WishlistEventPublisher publisher = new WishlistEventPublisher(eventPublisher);

        assertThatCode(() -> publisher.publishAdded(7L)).doesNotThrowAnyException();
        assertThatCode(() -> publisher.publishRemoved(7L)).doesNotThrowAnyException();
    }
}
