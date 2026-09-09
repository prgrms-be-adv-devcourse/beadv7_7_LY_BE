package site.memberservice.auth.infrastructure.redis;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import site.memberservice.auth.domain.LoginType;

@RequiredArgsConstructor
@Component
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "refresh-token:";

    private final StringRedisTemplate redisTemplate;

    public void save(final Long memberId, final LoginType loginType, final String value, final long ttlMillis) {
        redisTemplate.opsForValue().set(key(memberId, loginType), value, Duration.ofMillis(ttlMillis));
    }

    public boolean matches(final Long memberId, final LoginType loginType, final String value) {
        return value.equals(redisTemplate.opsForValue().get(key(memberId, loginType)));
    }

    public void remove(final Long memberId, final LoginType loginType, final String expectedValue) {
        final String key = key(memberId, loginType);
        if (expectedValue.equals(redisTemplate.opsForValue().get(key))) {
            redisTemplate.delete(key);
        }
    }

    private String key(final Long memberId, final LoginType loginType) {
        return KEY_PREFIX + memberId + ":" + loginType.name();
    }
}
