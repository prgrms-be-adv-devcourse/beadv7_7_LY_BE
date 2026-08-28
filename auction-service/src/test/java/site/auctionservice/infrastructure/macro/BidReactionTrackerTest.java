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
    @DisplayName("표본이 충분하고 평균 반응속도가 300ms 미만이면 위험 점수를 추가한다")
    void recordReactionIfApplicable_fastAndRegular_addsScore() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.opsForList()).willReturn(listOperations);
        given(valueOperations.getAndDelete(MARK_KEY)).willReturn(String.valueOf(System.currentTimeMillis() - 50));
        given(listOperations.range(HISTORY_KEY, 0, -1)).willReturn(List.of("50", "60", "45", "55", "48"));

        tracker.recordReactionIfApplicable(1L, 2L);

        verify(riskScoreManager).addScore(2L, 30);
    }

    @Test
    @DisplayName("표본이 충분해도 평균 반응속도가 300ms 이상이면 위험 점수를 추가하지 않는다")
    void recordReactionIfApplicable_slowReaction_doesNotAddScore() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.opsForList()).willReturn(listOperations);
        given(valueOperations.getAndDelete(MARK_KEY)).willReturn(String.valueOf(System.currentTimeMillis() - 1000));
        given(listOperations.range(HISTORY_KEY, 0, -1)).willReturn(List.of("1000", "1200", "900", "1100", "950"));

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
}
