package site.common.client;

import java.time.Duration;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import site.common.response.ApiResponse;

/**
 * 서비스 간 내부 API를 부르는 XxxHttpClient들이 반복하던 "호출 → 실패를 도메인 예외로 → data null 체크"를 한 곳에 모은다.
 * <p>
 * 재시도 여부를 메서드 이름으로 나눈다({@link #execute}·{@link #executeWithRetry}) — {@code RetryPolicy} 인자 하나로만
 * 구분하면 호출부만 봐서는 재시도가 걸려 있는지 알기 어렵고, 특히 쓰기 작업에 실수로 재시도가 켜지는 걸 코드 리뷰에서 놓치기 쉽다. 이름이 다르면 grep 한 번으로
 * 재시도가 걸린 호출을 전부 찾을 수 있다.
 * <p>
 * 실패를 어떤 예외로 바꿀지는 호출자가 결정한다({@code exceptionHandler}) — 클라이언트마다 자기 바운디드 컨텍스트의 ErrorCode로 감싸야 하기
 * 때문이다. 응답 본문이 비어 있는 경우도 같은 핸들러로 흘려보낸다 — 호출 자체는 성공했으므로 다시 불러도 같은 결과다.
 * <p>
 */
@Slf4j
public final class RestClientTemplate {

    private RestClientTemplate() {
    }

    /**
     * 재시도 없이 한 번만 시도한다.
     */
    public static <T> T execute(final Supplier<ApiResponse<T>> requestSupplier,
        final Function<RestClientException, RuntimeException> exceptionHandler) {
        try {
            return call(requestSupplier);
        } catch (final RestClientException e) {
            throw exceptionHandler.apply(e);
        }
    }

    /**
     * 재시도 설정 안하면 RetryPolicy.DEFAULT
     * 재시도 커스텀 설정 시 RetryPolicy를 인자로 받아 재시도 횟수, 딜레이를 커스텀할 수 있다.
     * Retry 발생 조건(5xx만 재시도)은 커스텀 불가
     * Retry에서 delay를 주는 방식은 thread sleep 이므로 사용하는 곳에서 주의 필요
     */
    public static <T> T executeWithRetry(final Supplier<ApiResponse<T>> requestSupplier,
        final Function<RestClientException, RuntimeException> exceptionHandler) {
        return executeWithRetry(requestSupplier, RetryPolicy.DEFAULT, exceptionHandler);
    }

    public static <T> T executeWithRetry(final Supplier<ApiResponse<T>> requestSupplier,
        final RetryPolicy retryPolicy,
        final Function<RestClientException, RuntimeException> exceptionHandler) {
        for (int attempt = 1; ; attempt++) {
            try {
                return call(requestSupplier);
            } catch (final RestClientException e) {
                if (!isRetryable(e) || attempt > retryPolicy.maxRetries()) {
                    throw exceptionHandler.apply(e);
                }
                final Duration delay = retryPolicy.backoff(attempt);
                log.warn("호출 실패, {}ms 뒤 재시도합니다 ({}/{}) — {}", delay.toMillis(), attempt,
                    retryPolicy.maxRetries(), e.getMessage());
                sleep(delay);
            }
        }
    }

    private static <T> T call(final Supplier<ApiResponse<T>> requestSupplier) {
        final ApiResponse<T> response = requestSupplier.get();
        if (response == null || response.getData() == null) {
            throw new RestClientException("응답 본문이 비어 있습니다.");
        }
        return response.getData();
    }

    //5xx만 재시도
    private static boolean isRetryable(final RestClientException e) {
        return e instanceof HttpServerErrorException || e instanceof ResourceAccessException;
    }

    private static void sleep(final Duration delay) {
        try {
            Thread.sleep(delay.toMillis());
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("재시도 대기 중 인터럽트됨", e);
        }
    }
}
