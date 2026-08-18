package site.pointwalletservice.outbox.application;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.pointwalletservice.outbox.domain.OutboxEvent;
import site.pointwalletservice.outbox.domain.OutboxEventRepository;
import site.pointwalletservice.outbox.domain.OutboxEventStatus;
import site.pointwalletservice.outbox.exception.OutboxException;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxAdminService")
class OutboxAdminServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private OutboxAdminService sut;

    @BeforeEach
    void setUp() {
        sut = new OutboxAdminService(outboxEventRepository);
    }

    @Test
    @DisplayName("findDeadEvents는 리포지토리의 DEAD 최신순 조회를 그대로 위임한다")
    void findDeadEvents_리포지토리에_위임한다() {
        OutboxEvent dead = OutboxEvent.create("topic", "key", "some.EventType", "{}");
        when(outboxEventRepository.findDeadNewestFirst()).thenReturn(List.of(dead));

        List<OutboxEvent> result = sut.findDeadEvents();

        assertThat(result).containsExactly(dead);
    }

    @Test
    @DisplayName("retryManually는 대상 이벤트를 찾아 도메인 재시도 메서드를 호출하고 저장한다")
    void retryManually_이벤트를_찾아_재시도처리하고_저장한다() {
        OutboxEvent dead = OutboxEvent.create("topic", "key", "some.EventType", "{}");
        for (int i = 0; i < OutboxEvent.MAX_RETRY_COUNT; i++) {
            dead.markFailed("실패");
        }
        when(outboxEventRepository.findById(1L)).thenReturn(Optional.of(dead));

        sut.retryManually(1L);

        assertThat(dead.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(dead.getRetryCount()).isEqualTo(0);
        verify(outboxEventRepository).save(dead);
    }

    @Test
    @DisplayName("존재하지 않는 id면 예외가 발생한다")
    void retryManually_존재하지_않으면_예외() {
        when(outboxEventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.retryManually(999L))
                .isInstanceOf(OutboxException.class);
    }
}