package site.auctionservice.infrastructure.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;

/**
 * ZSET(score=timestamp)에 요청을 개별 기록해 윈도우 내 정확한 요청 수를 세는 Rate Limiter.
 * "확인 + 기록"을 Lua로 원자화해 동시 요청 간 race condition을 막는다.
 * EVALSHA/NOSCRIPT 재시도는 StringRedisTemplate.execute(RedisScript, ...)가 내부적으로 처리해준다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlidingWindowLogLimiter {

    // KEYS[1]=key, ARGV[1]=now(ms), ARGV[2]=window(ms), ARGV[3]=limit, ARGV[4]=member
    private static final RedisScript<Long> SCRIPT = new DefaultRedisScript<>("""
            local key = KEYS[1]
            local now = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local limit = tonumber(ARGV[3])
            local member = ARGV[4]

            local windowStart = now - window
            redis.call('ZREMRANGEBYSCORE', key, 0, windowStart)
            local count = redis.call('ZCARD', key)

            if count < limit then
                redis.call('ZADD', key, now, member)
                redis.call('PEXPIRE', key, window)
                return 1
            else
                return 0
            end
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    /**
     * @param key      제한 대상 (예: "auction:antibot:ratelimit:bid:{auctionId}:{bidderId}")
     * @param limit    윈도우 내 최대 허용 요청 수
     * @param windowMs 윈도우 크기(밀리초)
     * @return true=허용, false=차단
     */
    public boolean isAllowed(String key, int limit, long windowMs) {
        long now = System.currentTimeMillis();
        String member = now + ":" + UUID.randomUUID();

        // rate limit은 어뷰징 방어용 부가 기능일 뿐 핵심 입찰 흐름의 정합성 요건이 아니므로 Redis 장애 시 fail-open(허용)한다
        try {
            Long result = redisTemplate.execute(SCRIPT, Collections.singletonList(key),
                    String.valueOf(now), String.valueOf(windowMs), String.valueOf(limit), member);
            return result != null && result == 1L;
        } catch (RuntimeException e) {
            log.warn("Rate limit 확인 중 Redis 오류로 fail-open 처리: key={}", key, e);
            return true;
        }
    }
}
