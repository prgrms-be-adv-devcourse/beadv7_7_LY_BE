package site.auctionservice.infrastructure.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class SlidingWindowLogLimiterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private SlidingWindowLogLimiter limiter;

    @Test
    @DisplayName("스크립트가 1을 반환하면 허용한다")
    void isAllowed_scriptReturnsOne_allows() {
        given(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any(), any()))
                .willReturn(1L);

        boolean allowed = limiter.isAllowed("auction:antibot:ratelimit:bid:1:2", 3, 2000L);

        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("스크립트가 0을 반환하면 차단한다")
    void isAllowed_scriptReturnsZero_blocks() {
        given(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any(), any()))
                .willReturn(0L);

        boolean allowed = limiter.isAllowed("auction:antibot:ratelimit:bid:1:2", 3, 2000L);

        assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("스크립트 실행 결과가 null이면 차단한다")
    void isAllowed_nullResult_blocks() {
        given(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any(), any()))
                .willReturn(null);

        boolean allowed = limiter.isAllowed("auction:antibot:ratelimit:bid:1:2", 3, 2000L);

        assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("Redis 장애 시 fail-open으로 허용한다")
    void isAllowed_redisFailure_failsOpen() {
        willThrow(new RedisConnectionFailureException("connection refused"))
                .given(redisTemplate).execute(any(RedisScript.class), anyList(), any(), any(), any(), any());

        boolean allowed = limiter.isAllowed("auction:antibot:ratelimit:bid:1:2", 3, 2000L);

        assertThat(allowed).isTrue();
    }
}
