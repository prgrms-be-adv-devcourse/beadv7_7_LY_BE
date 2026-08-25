package site.explorationservice.recommendation.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.explorationservice.recommendation.domain.DueMember;

/**
 * 시간 경계(디바운스 임계값)와 동시성 시나리오(처리 중 재클레임 방지, 처리 도중 새 변경)를 실제 sleep 없이 검증하려고 {@link Clock}을 주입한다.
 */
@DisplayName("인메모리 dirty 추적")
class InMemoryDirtyMemberTrackerTest {

    private static final Duration DEBOUNCE = Duration.ofSeconds(30);

    private final AtomicReference<Instant> now = new AtomicReference<>(
        Instant.parse("2026-01-01T00:00:00Z"));
    private final InMemoryDirtyMemberTracker tracker =
        new InMemoryDirtyMemberTracker(new AtomicReferenceClock(now));

    @Test
    @DisplayName("디바운스 윈도우를 지나지 않으면 클레임 대상이 아니다")
    void 윈도우_전이면_안_잡힘() {
        tracker.markDirty(1L, now.get());
        advance(Duration.ofSeconds(10));

        assertThat(tracker.claimDue(DEBOUNCE)).isEmpty();
    }

    @Test
    @DisplayName("디바운스 윈도우를 지나면 클레임된다")
    void 윈도우_지나면_잡힘() {
        tracker.markDirty(1L, now.get());
        advance(Duration.ofSeconds(31));

        final List<DueMember> due = tracker.claimDue(DEBOUNCE);

        assertThat(due).extracting(DueMember::memberId).containsExactly(1L);
    }

    @Test
    @DisplayName("이미 처리 중인 멤버는 다음 클레임에서 다시 반환되지 않는다")
    void 처리중이면_중복_클레임_안됨() {
        tracker.markDirty(1L, now.get());
        advance(Duration.ofSeconds(31));

        assertThat(tracker.claimDue(DEBOUNCE)).hasSize(1);
        assertThat(tracker.claimDue(DEBOUNCE)).isEmpty();
    }

    @Test
    @DisplayName("성공 완료 시 관찰값이 그대로면 dirty 상태가 정리되고, release 없이도 재클레임 가능해진다")
    void complete_성공() {
        tracker.markDirty(1L, now.get());
        advance(Duration.ofSeconds(31));
        final DueMember due = tracker.claimDue(DEBOUNCE).get(0);

        tracker.complete(due.memberId(), due.dirtySince());

        advance(Duration.ofSeconds(31));
        assertThat(tracker.claimDue(DEBOUNCE)).isEmpty();
    }

    @Test
    @DisplayName("처리 도중 새 변경이 오면 complete가 dirty 상태를 지우지 않는다")
    void complete_도중_새_변경() {
        tracker.markDirty(1L, now.get());
        advance(Duration.ofSeconds(31));
        final DueMember due = tracker.claimDue(DEBOUNCE).get(0);

        tracker.markDirty(1L, now.get()); // 처리 도중 새 이벤트
        tracker.complete(due.memberId(), due.dirtySince()); // 옛 값으로 완료 시도 — CAS 실패해야 함

        advance(Duration.ofSeconds(31));
        assertThat(tracker.claimDue(DEBOUNCE)).extracting(DueMember::memberId).containsExactly(1L);
    }

    @Test
    @DisplayName("실패 시 release만 하면 dirty 상태가 남아 다음 클레임에서 재시도된다")
    void release_실패_재시도() {
        tracker.markDirty(1L, now.get());
        advance(Duration.ofSeconds(31));
        final DueMember due = tracker.claimDue(DEBOUNCE).get(0);

        tracker.release(due.memberId()); // LLM 호출 실패 시나리오 — dirty는 안 건드림

        assertThat(tracker.claimDue(DEBOUNCE)).extracting(DueMember::memberId).containsExactly(1L);
    }

    private void advance(final Duration by) {
        now.updateAndGet(instant -> instant.plus(by));
    }

    /**
     * 테스트에서 시간을 임의로 앞당길 수 있도록 {@link AtomicReference}를 감싼 {@link Clock}. {@code Clock}은 추상 클래스라
     * record로는 못 만든다.
     */
    private static final class AtomicReferenceClock extends Clock {

        private final AtomicReference<Instant> instant;

        private AtomicReferenceClock(final AtomicReference<Instant> instant) {
            this.instant = instant;
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(final java.time.ZoneId zone) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Instant instant() {
            return instant.get();
        }
    }
}
