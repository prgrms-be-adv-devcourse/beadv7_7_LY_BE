package site.pointwalletservice.outbox.application;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.pointwalletservice.outbox.domain.OutboxEvent;
import site.pointwalletservice.outbox.domain.OutboxEventRepository;
import site.pointwalletservice.withdraw.domain.event.WithdrawFeeEarnedEvent;

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

    @Test
    @DisplayName("이벤트를 JSON으로 직렬화해 PENDING 상태의 OutboxEvent로 저장한다")
    void store_이벤트를_직렬화해_저장한다() {
        // given
        WithdrawFeeEarnedEvent event = new WithdrawFeeEarnedEvent(1L, java.math.BigDecimal.valueOf(2_000));

        // when
        sut.store("pointwallet.withdraw-fee-earned", "1", event);

        // then
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent saved = captor.getValue();
        assertThat(saved.getTopic()).isEqualTo("pointwallet.withdraw-fee-earned");
        assertThat(saved.getPartitionKey()).isEqualTo("1");
        assertThat(saved.getEventType()).isEqualTo(WithdrawFeeEarnedEvent.class.getName());
        assertThat(saved.getPayload()).contains("\"withdrawId\":1").contains("2000");
        assertThat(saved.getStatus()).isEqualTo(site.pointwalletservice.outbox.domain.OutboxEventStatus.PENDING);
    }
}