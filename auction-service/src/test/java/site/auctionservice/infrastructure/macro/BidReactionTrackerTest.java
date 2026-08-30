package site.auctionservice.infrastructure.macro;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class BidReactionTrackerTest {

    private static final String MARK_KEY = "auction:antibot:outbidmark:1:2";
    private static final String HISTORY_KEY = "auction:antibot:reaction:2";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ListOperations<String, String> listOperations;

    @Mock
    private BidRiskScoreManager riskScoreManager;

    @InjectMocks
    private BidReactionTracker tracker;

    @Test
    @DisplayName("outbid 이력이 없으면(최초 입찰) 아무 것도 기록하지 않는다")
    void recordReactionIfApplicable_noMark_doesNothing() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.getAndDelete(MARK_KEY)).willReturn(null);

        tracker.recordReactionIfApplicable(1L, 2L);

        verifyNoInteractions(listOperations);
        verifyNoInteractions(riskScoreManager);
    }

    @Test
    @DisplayName("outbid 이력이 있으면 반응속도를 이력(LIST)에 기록하고 TTL을 갱신한다")
    void recordReactionIfApplicable_withMark_recordsHistory() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.opsForList()).willReturn(listOperations);
        given(valueOperations.getAndDelete(MARK_KEY)).willReturn(String.valueOf(System.currentTimeMillis() - 500));
        given(listOperations.range(HISTORY_KEY, 0, -1)).willReturn(List.of("500"));

        tracker.recordReactionIfApplicable(1L, 2L);

        verify(listOperations).leftPush(eq(HISTORY_KEY), anyString());
        verify(listOperations).trim(HISTORY_KEY, 0, 4);
        verify(redisTemplate).expire(eq(HISTORY_KEY), any(Duration.class));
    }

    @Test
    @DisplayName("표본이 5개 미만이면 판단을 보류하고 위험 점수를 매기지 않는다")
    void recordReactionIfApplicable_insufficientSample_doesNotAddScore() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.opsForList()).willReturn(listOperations);
        given(valueOperations.getAndDelete(MARK_KEY)).willReturn(String.valueOf(System.currentTimeMillis() - 100));
        given(listOperations.range(HISTORY_KEY, 0, -1)).willReturn(List.of("100", "120", "90"));

        tracker.recordReactionIfApplicable(1L, 2L);

        verifyNoInteractions(riskScoreManager);
    }

    @Test
    @DisplayName("평균이 150ms 미만이면 편차와 무관하게 위험 점수를 추가한다 (사람 반응시간 물리적 하한 아래)")
    void recordReactionIfApplicable_absolutelyTooFast_addsScore() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.opsForList()).willReturn(listOperations);
        given(valueOperations.getAndDelete(MARK_KEY)).willReturn(String.valueOf(System.currentTimeMillis() - 50));
        given(listOperations.range(HISTORY_KEY, 0, -1)).willReturn(List.of("50", "60", "45", "55", "48"));

        tracker.recordReactionIfApplicable(1L, 2L);

        verify(riskScoreManager).addScore(2L, 30);
    }

    @Test
    @DisplayName("평균은 사람도 낼 수 있는 수준이어도 편차(CV)가 기계적으로 일정하면 위험 점수를 추가한다")
    void recordReactionIfApplicable_slowButMechanicallyRegular_addsScore() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.opsForList()).willReturn(listOperations);
        given(valueOperations.getAndDelete(MARK_KEY)).willReturn(String.valueOf(System.currentTimeMillis() - 500));
        given(listOperations.range(HISTORY_KEY, 0, -1)).willReturn(List.of("500", "510", "495", "505", "498"));

        tracker.recordReactionIfApplicable(1L, 2L);

        verify(riskScoreManager).addScore(2L, 30);
    }

    @Test
    @DisplayName("평균이 사람 범위이고 편차도 사람처럼 들쭉날쭉하면 위험 점수를 추가하지 않는다")
    void recordReactionIfApplicable_humanLikeVariance_doesNotAddScore() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.opsForList()).willReturn(listOperations);
        given(valueOperations.getAndDelete(MARK_KEY)).willReturn(String.valueOf(System.currentTimeMillis() - 500));
        given(listOperations.range(HISTORY_KEY, 0, -1)).willReturn(List.of("500", "1200", "300", "900", "700"));

        tracker.recordReactionIfApplicable(1L, 2L);

        verify(riskScoreManager, never()).addScore(any(), anyInt());
    }

    @Test
    @DisplayName("평균이 2초 이상이면 아무리 일정해도 위험 점수를 추가하지 않는다 (느리지만 습관적인 사람 배제)")
    void recordReactionIfApplicable_slowButRegular_doesNotAddScore() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.opsForList()).willReturn(listOperations);
        given(valueOperations.getAndDelete(MARK_KEY)).willReturn(String.valueOf(System.currentTimeMillis() - 2000));
        given(listOperations.range(HISTORY_KEY, 0, -1)).willReturn(List.of("2000", "2010", "1995", "2005", "1998"));

        tracker.recordReactionIfApplicable(1L, 2L);

        verify(riskScoreManager, never()).addScore(any(), anyInt());
    }

    @Test
    @DisplayName("Redis 오류가 나도 예외를 전파하지 않는다")
    void recordReactionIfApplicable_redisFailure_doesNotThrow() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        willThrow(new RedisConnectionFailureException("connection refused"))
                .given(valueOperations).getAndDelete(MARK_KEY);

        tracker.recordReactionIfApplicable(1L, 2L);
    }

    @Test
    @DisplayName("마킹 값이 손상돼 파싱에 실패해도(NumberFormatException) 예외를 전파하지 않는다")
    void recordReactionIfApplicable_corruptedMark_doesNotThrow() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.getAndDelete(MARK_KEY)).willReturn("not-a-number");

        tracker.recordReactionIfApplicable(1L, 2L);

        verifyNoInteractions(riskScoreManager);
    }
}
