package site.explorationservice.recommendation.infrastructure;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import site.explorationservice.recommendation.domain.DirtyMemberTracker;
import site.explorationservice.recommendation.domain.DueMember;

/**
 * dirty 상태를 인스턴스 로컬 메모리로만 관리한다. memberId로 파티셔닝된 Kafka 이벤트를 소비하는 인스턴스가 곧 그 멤버를 처리할 인스턴스라 크로스 인스턴스
 * 공유가 필요 없고, 인스턴스 재시작 시 유실은 감수하기로 했다 — 다음 실제 위시리스트 변경 때 회복된다
 * <p>
 * {@code dirtySince}: 디바운스 타이밍(언제부터 조용해지길 기다리는지). {@code inFlight}: 스윕 틱 간 중복 재클레임 방지 — 처리 중인 멤버가
 * (아직 안 끝났는데) 다음 틱에서 또 집히는 걸 막는다.
 */
@Component
public class InMemoryDirtyMemberTracker implements DirtyMemberTracker {

    private final ConcurrentHashMap<Long, Instant> dirtySince = new ConcurrentHashMap<>();
    private final Set<Long> inFlight = ConcurrentHashMap.newKeySet();
    private final Clock clock;

    public InMemoryDirtyMemberTracker() {
        this(Clock.systemUTC());
    }

    InMemoryDirtyMemberTracker(final Clock clock) {
        this.clock = clock;
    }

    @Override
    public void markDirty(final Long memberId, final Instant occurredAt) {
        dirtySince.put(memberId, occurredAt);
    }

    @Override
    public List<DueMember> claimDue(final Duration debounceWindow) {
        final Instant threshold = Instant.now(clock).minus(debounceWindow);
        final List<DueMember> due = new ArrayList<>();

        dirtySince.forEach((memberId, changedAt) -> {
            if (changedAt.isBefore(threshold) && inFlight.add(memberId)) {
                due.add(new DueMember(memberId, changedAt));
            }
        });

        return due;
    }

    @Override
    public void complete(final Long memberId, final Instant observedDirtySince) {
        dirtySince.remove(memberId, observedDirtySince);
        inFlight.remove(memberId);
    }

    @Override
    public void release(final Long memberId) {
        inFlight.remove(memberId);
    }
}
