package site.explorationservice.recommendation.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.common.event.contract.WishlistChangedEvent;
import site.explorationservice.recommendation.domain.DirtyMemberTracker;

@ExtendWith(MockitoExtension.class)
@DisplayName("WishlistChangedEventListener")
class WishlistChangedEventListenerTest {

    private static final Long MEMBER_ID = 42L;

    @Mock
    private DirtyMemberTracker dirtyMemberTracker;

    @InjectMocks
    private WishlistChangedEventListener sut;

    @Captor
    private ArgumentCaptor<Instant> occurredAtCaptor;

    @Test
    @DisplayName("이벤트를 받으면 memberId와 occurredAt(zone 변환)으로 markDirty를 호출한다")
    void handle_markDirty_위임() {
        final LocalDateTime occurredAt = LocalDateTime.now();
        final WishlistChangedEvent event = WishlistChangedEvent.builder()
            .eventId(UUID.randomUUID())
            .occurredAt(occurredAt)
            .memberId(MEMBER_ID)
            .build();

        sut.handle(event);

        verify(dirtyMemberTracker).markDirty(eq(MEMBER_ID), occurredAtCaptor.capture());
        // 컨슈머 수신 시각이 아니라 이벤트의 occurredAt(JVM 기본 zone 기준)이 그대로 넘어가야 한다.
        assertThat(occurredAtCaptor.getValue())
            .isEqualTo(occurredAt.atZone(ZoneId.systemDefault()).toInstant());
    }
}
