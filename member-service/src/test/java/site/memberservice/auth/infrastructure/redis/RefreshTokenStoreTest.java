package site.memberservice.auth.infrastructure.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import site.memberservice.auth.domain.LoginType;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RefreshTokenStoreTest {

    private static final String KEY = "refresh-token:1727:NORMAL";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RefreshTokenStore refreshTokenStore;

    @DisplayName("save는 회원+로그인타입 키에 TTL과 함께 값을 저장한다.")
    @Test
    void save() {
        // Given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // When
        refreshTokenStore.save(1727L, LoginType.NORMAL, "TOKEN_VALUE", Duration.ofDays(7).toMillis());

        // Then
        verify(valueOperations).set(KEY, "TOKEN_VALUE", Duration.ofDays(7));
    }

    @DisplayName("matches는 저장된 값과 일치하면 true를 반환한다.")
    @Test
    void matchesReturnsTrueWhenValueEquals() {
        // Given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(KEY)).willReturn("TOKEN_VALUE");

        // When & Then
        assertThat(refreshTokenStore.matches(1727L, LoginType.NORMAL, "TOKEN_VALUE")).isTrue();
    }

    @DisplayName("matches는 저장된 값과 다르거나 키가 없으면 false를 반환한다.")
    @Test
    void matchesReturnsFalseWhenValueDiffersOrMissing() {
        // Given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(KEY)).willReturn(null);

        // When & Then
        assertThat(refreshTokenStore.matches(1727L, LoginType.NORMAL, "TOKEN_VALUE")).isFalse();
    }

    @DisplayName("remove는 저장된 값이 예상 값과 일치할 때만 키를 삭제한다.")
    @Test
    void removeDeletesOnlyWhenValueMatches() {
        // Given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(KEY)).willReturn("TOKEN_VALUE");

        // When
        refreshTokenStore.remove(1727L, LoginType.NORMAL, "TOKEN_VALUE");

        // Then
        verify(redisTemplate).delete(KEY);
    }

    @DisplayName("remove는 저장된 값이 예상 값과 다르면(이미 재로그인으로 덮어써졌으면) 삭제하지 않는다.")
    @Test
    void removeSkipsDeletionWhenValueDoesNotMatch() {
        // Given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(KEY)).willReturn("NEWER_TOKEN_VALUE");

        // When
        refreshTokenStore.remove(1727L, LoginType.NORMAL, "OLD_TOKEN_VALUE");

        // Then
        verify(redisTemplate, never()).delete(KEY);
    }
}
