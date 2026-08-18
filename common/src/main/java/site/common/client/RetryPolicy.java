package site.common.client;

import java.time.Duration;

/**
 * baseDelay * multiplier^(시도-1)로 커지는 지수 백오프, maxDelay로 상한을 둔다.
 * <p>
 * <b>멱등한 호출에만 쓴다.</b> 응답이 유실됐을 뿐 상대는 이미 처리를 끝냈을 수 있는데, 여기 태우면 같은 요청이 그대로
 * 중복 실행된다 — GET처럼 결과가 몇 번을 다시 보내도 같은 조회일 때만 안전하다.
 */
public record RetryPolicy(int maxRetries, Duration baseDelay, Duration maxDelay,
                          double multiplier) {

    // 2회 재시도, 100ms 지연 후 2배씩 증가, 최대 500ms 지연
    public static final RetryPolicy DEFAULT =
        new RetryPolicy(2, Duration.ofMillis(100), Duration.ofMillis(500), 2.0);

    Duration backoff(final int attempt) {
        final long exponential = (long) (baseDelay.toMillis() * Math.pow(multiplier, attempt - 1));
        return Duration.ofMillis(Math.min(exponential, maxDelay.toMillis()));
    }
}
