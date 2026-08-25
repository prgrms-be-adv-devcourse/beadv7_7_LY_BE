package site.explorationservice.recommendation.infrastructure;

import java.time.Instant;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import site.common.event.contract.WishlistChangedEvent;
import site.explorationservice.recommendation.domain.DirtyMemberTracker;

/**
 * 위시리스트 변경을 dirty 표시로만 남긴다 — 실제 재계산은 스윕 스케줄러가 담당한다.
 * <p>
 * dirty 타임스탬프로 컨슈머 수신 시각이 아니라 {@code event.occurredAt}(실제 위시리스트가 바뀐 시각)을 쓴다. {@code occurredAt}은
 * {@code LocalDateTime}이라 zone 정보가 없어 변환이 필요한데, 여기선 이 서비스의 JVM 기본 zone을 그대로 쓴다 — <b>모든 컨테이너가 같은
 * zone(Asia/Seoul)을 쓴다는 전제</b>다. `common.event.Event`의 `occurredAt`도 `LocalDateTime.now()`(JVM 기본
 * zone)로 채번되므로 지금은 이 전제가 성립하지만, 서비스별로 zone이 달라지면 조용히 틀어질 수 있다.
 */
@Component
@RequiredArgsConstructor
public class WishlistChangedEventListener {

    private final DirtyMemberTracker dirtyMemberTracker;

    @KafkaListener(topics = "#{T(site.common.event.contract.EventType).WISHLIST_CHANGED_EVENT.getValue()}",
        groupId = "exploration-service")
    public void handle(final WishlistChangedEvent event) {
        final Instant occurredAt = event.getOccurredAt().atZone(ZoneId.systemDefault()).toInstant();
        dirtyMemberTracker.markDirty(event.getMemberId(), occurredAt);
    }
}
