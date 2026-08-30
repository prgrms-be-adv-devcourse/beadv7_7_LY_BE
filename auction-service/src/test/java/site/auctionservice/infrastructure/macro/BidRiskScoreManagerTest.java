package site.auctionservice.infrastructure.macro;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BidRiskScoreManagerTest {

    private static final String KEY = "auction:antibot:risk:2";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private BidRiskScoreManager riskScoreManager;

    @Test
    @DisplayName("점수가 최초 생성될 때(newScore == delta)만 TTL을 설정한다")
    void addScore_firstScore_setsTtl() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment(KEY, 30)).willReturn(30L);

        riskScoreManager.addScore(2L, 30);

        verify(redisTemplate).expire(org.mockito.ArgumentMatchers.eq(KEY), any(Duration.class));
    }

    @Test
    @DisplayName("이미 점수가 쌓여있으면 TTL을 다시 설정하지 않는다")
    void addScore_existingScore_doesNotResetTtl() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment(KEY, 30)).willReturn(60L);

        riskScoreManager.addScore(2L, 30);

        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("Redis 오류 시 점수 갱신 실패를 삼키고 예외를 전파하지 않는다")
    void addScore_redisFailure_doesNotThrow() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        willThrow(new RedisConnectionFailureException("connection refused"))
                .given(valueOperations).increment(KEY, 30);

        riskScoreManager.addScore(2L, 30);

        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("점수가 80 이상이면 limit을 1로 조인다")
    void adjustLimit_highRisk_returnsOne() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(KEY)).willReturn("80");

        int limit = riskScoreManager.adjustLimit(10, 2L);

        assertThat(limit).isEqualTo(1);
    }

    @Test
    @DisplayName("점수가 40 이상 80 미만이면 limit을 절반으로 조인다")
    void adjustLimit_mediumRisk_halvesLimit() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(KEY)).willReturn("40");

        int limit = riskScoreManager.adjustLimit(10, 2L);

        assertThat(limit).isEqualTo(5);
    }

    @Test
    @DisplayName("점수가 낮으면 baseLimit을 그대로 반환한다")
    void adjustLimit_lowRisk_returnsBaseLimit() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(KEY)).willReturn(null);

        int limit = riskScoreManager.adjustLimit(3, 2L);

        assertThat(limit).isEqualTo(3);
    }

    @Test
    @DisplayName("Redis 조회 실패 시 fail-open으로 baseLimit을 그대로 반환한다")
    void adjustLimit_redisFailure_failsOpen() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        willThrow(new RedisConnectionFailureException("connection refused")).given(valueOperations).get(KEY);

        int limit = riskScoreManager.adjustLimit(3, 2L);

        assertThat(limit).isEqualTo(3);
    }
}
