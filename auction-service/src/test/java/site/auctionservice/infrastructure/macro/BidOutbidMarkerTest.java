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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BidOutbidMarkerTest {

    private static final String KEY = "auction:antibot:outbidmark:1:5";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private BidOutbidMarker marker;

    @Test
    @DisplayName("(경매, 이전 최고입찰자) 단위로 현재 시각을 마킹한다")
    void markOutbid_marksTimestamp() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        marker.markOutbid(1L, 5L);

        verify(valueOperations).set(eq(KEY), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("Redis 오류가 나도 예외를 전파하지 않는다 (매크로 탐지 신호 손실일 뿐 입찰엔 영향 없음)")
    void markOutbid_redisFailure_doesNotThrow() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        willThrow(new RedisConnectionFailureException("connection refused"))
                .given(valueOperations).set(eq(KEY), anyString(), any(Duration.class));

        marker.markOutbid(1L, 5L);
    }

    @Test
    @DisplayName("DataAccessException이 아닌 RuntimeException이 나도 예외를 전파하지 않는다")
    void markOutbid_unexpectedRuntimeException_doesNotThrow() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        willThrow(new IllegalStateException("unexpected"))
                .given(valueOperations).set(eq(KEY), anyString(), any(Duration.class));

        marker.markOutbid(1L, 5L);
    }
}
