package site.explorationservice.recommendation.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.explorationservice.recommendation.domain.DirtyMemberTracker;
import site.explorationservice.recommendation.domain.DueMember;
import site.explorationservice.recommendation.domain.RecommendationPolicy;

@ExtendWith(MockitoExtension.class)
@DisplayName("가중치 재계산 스윕")
class InterestWeightSweepSchedulerTest {

    @Mock
    private DirtyMemberTracker dirtyMemberTracker;

    @Mock
    private InterestWeightRecomputeService interestWeightRecomputeService;

    @Mock
    private Executor interestWeightRecomputeExecutor;

    @InjectMocks
    private InterestWeightSweepScheduler sut;

    @Test
    @DisplayName("클레임된 멤버 각각을 executor에 제출한다")
    void 클레임된_멤버_제출() {
        final DueMember due1 = new DueMember(1L, Instant.now());
        final DueMember due2 = new DueMember(2L, Instant.now());
        given(dirtyMemberTracker.claimDue(RecommendationPolicy.DEBOUNCE_WINDOW))
            .willReturn(List.of(due1, due2));

        sut.sweep();

        then(interestWeightRecomputeExecutor).should(org.mockito.Mockito.times(2))
            .execute(any());
    }

    @Test
    @DisplayName("작업 제출 자체가 거부되면 release로 클레임을 풀어준다 — 안 그러면 영구히 재클레임 못 한다")
    void 제출_거부시_release() {
        final DueMember due = new DueMember(1L, Instant.now());
        given(dirtyMemberTracker.claimDue(RecommendationPolicy.DEBOUNCE_WINDOW))
            .willReturn(List.of(due));
        doThrow(new RejectedExecutionException("큐 꽉 참"))
            .when(interestWeightRecomputeExecutor).execute(any());

        sut.sweep();

        then(dirtyMemberTracker).should().release(1L);
    }

    @Test
    @DisplayName("클레임된 멤버가 없으면 아무것도 제출하지 않는다")
    void 클레임_없음() {
        given(dirtyMemberTracker.claimDue(RecommendationPolicy.DEBOUNCE_WINDOW))
            .willReturn(List.of());

        sut.sweep();

        then(interestWeightRecomputeExecutor).should(org.mockito.Mockito.never()).execute(any());
    }
}
