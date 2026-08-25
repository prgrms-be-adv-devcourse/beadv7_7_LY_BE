package site.explorationservice.recommendation.application;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import site.explorationservice.ai.chat.domain.ChatCallGate;
import site.explorationservice.recommendation.domain.DirtyMemberTracker;
import site.explorationservice.recommendation.domain.DueMember;
import site.explorationservice.recommendation.domain.RecommendationPolicy;

/**
 * 디바운스 윈도우를 지난 멤버들을 주기적으로 훑어 가중치 재계산을 트리거한다.
 * <p>
 * 시작 전에 {@link ChatCallGate}가 열려 있는지 먼저 확인한다 — 닫혀 있으면(장애 대응용 수동 스위치) {@code claimDue}를 호출하지 않음.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterestWeightSweepScheduler {

    private static final long SWEEP_INTERVAL_MILLIS = 10_000L;

    private final DirtyMemberTracker dirtyMemberTracker;
    private final InterestWeightRecomputeService interestWeightRecomputeService;
    private final Executor interestWeightRecomputeExecutor;
    private final ChatCallGate chatCallGate;

    @Scheduled(fixedDelay = SWEEP_INTERVAL_MILLIS)
    public void sweep() {
        if (!chatCallGate.isOpen()) {
            return;
        }

        final List<DueMember> due = dirtyMemberTracker.claimDue(
            RecommendationPolicy.DEBOUNCE_WINDOW);

        for (final DueMember member : due) {
            try {
                interestWeightRecomputeExecutor.execute(
                    () -> interestWeightRecomputeService.recompute(member));
            } catch (final RejectedExecutionException e) {
                // claimDue()에서 이미 "처리 중"으로 표시해뒀으므로, 쓰레드풀 획득 실패 시 release()
                log.warn("가중치 재계산 작업 제출 실패, 다음 스윕에서 재시도 — memberId={}", member.memberId(), e);
                dirtyMemberTracker.release(member.memberId());
            }
        }
    }
}
