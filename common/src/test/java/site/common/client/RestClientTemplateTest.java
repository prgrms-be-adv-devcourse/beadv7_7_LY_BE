package site.common.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import site.common.response.ApiResponse;

/**
 * 이 네 가지가 각 XxxHttpClient에서 반복되던 것들이다 — 여기서 한 번 검증해두면 클라이언트별 테스트는 URI·예외 매핑만 확인하면 된다.
 */
class RestClientTemplateTest {

    private record Payload(String value) {

    }

    @Test
    @DisplayName("성공하면 data를 그대로 돌려준다")
    void 성공() {
        final Payload result = RestClientTemplate.execute(
            () -> ApiResponse.success(new Payload("ok")),
            e -> new IllegalStateException("호출되면 안 됨", e));

        assertThat(result).isEqualTo(new Payload("ok"));
    }

    @Test
    @DisplayName("호출이 실패하면 exceptionHandler가 만든 예외를 던진다")
    void 호출_실패() {
        assertThatThrownBy(() -> RestClientTemplate.<Payload>execute(
            () -> {
                throw new RestClientException("연결 실패");
            },
            e -> new IllegalStateException("변환됨: " + e.getMessage())))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("변환됨: 연결 실패");
    }

    @Test
    @DisplayName("응답이 null이면 exceptionHandler로 넘긴다")
    void 응답_없음() {
        assertThatThrownBy(() -> RestClientTemplate.<Payload>execute(
            () -> null,
            e -> new IllegalStateException("변환됨")))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("data가 null이면 exceptionHandler로 넘긴다")
    void 데이터_없음() {
        assertThatThrownBy(() -> RestClientTemplate.<Payload>execute(
            () -> ApiResponse.success(null),
            e -> new IllegalStateException("변환됨")))
            .isInstanceOf(IllegalStateException.class);
    }

    /**
     * execute()와 별도 이름을 쓰는 이유(재시도 여부가 호출부에서 바로 보여야 함)와 맞물려, 재시도 자체도 여기서 확실히 검증해둔다 — 재시도 대상
     * 판단(5xx·연결 실패만)이 틀리면 4xx에도 재시도가 걸려 장애 중인 서비스에 트래픽만 더 얹는 사고로 이어진다.
     */
    @Nested
    @DisplayName("executeWithRetry")
    class ExecuteWithRetryTest {

        private final RetryPolicy fastRetry = new RetryPolicy(3, Duration.ofMillis(1),
            Duration.ofMillis(1), 1);

        @Test
        @DisplayName("재시도 가능한 실패 뒤 성공하면 그 결과를 돌려준다")
        void 재시도_후_성공() {
            final AtomicInteger calls = new AtomicInteger();

            final Payload result = RestClientTemplate.executeWithRetry(
                () -> {
                    if (calls.getAndIncrement() < 2) {
                        throw new ResourceAccessException("연결 실패");
                    }
                    return ApiResponse.success(new Payload("ok"));
                },
                fastRetry,
                e -> new IllegalStateException("호출되면 안 됨", e));

            assertThat(result).isEqualTo(new Payload("ok"));
            assertThat(calls.get()).isEqualTo(3);
        }

        @Test
        @DisplayName("재시도를 다 써도 실패하면 exceptionHandler가 만든 예외를 던진다")
        void 재시도_소진() {
            final AtomicInteger calls = new AtomicInteger();
            final RetryPolicy policy = new RetryPolicy(2, Duration.ofMillis(1),
                Duration.ofMillis(1), 1);

            assertThatThrownBy(() -> RestClientTemplate.<Payload>executeWithRetry(
                () -> {
                    calls.incrementAndGet();
                    throw new ResourceAccessException("연결 실패");
                },
                policy,
                e -> new IllegalStateException("변환됨")))
                .isInstanceOf(IllegalStateException.class);

            // 최초 시도 1 + 재시도 2 = 3.
            assertThat(calls.get()).isEqualTo(3);
        }

        @Test
        @DisplayName("재시도 대상이 아닌 실패는 다시 시도하지 않고 바로 넘긴다")
        void 재시도_대상_아님() {
            final AtomicInteger calls = new AtomicInteger();

            assertThatThrownBy(() -> RestClientTemplate.<Payload>executeWithRetry(
                () -> {
                    calls.incrementAndGet();
                    throw new RestClientException("설정 오류");
                },
                RetryPolicy.DEFAULT,
                e -> new IllegalStateException("변환됨")))
                .isInstanceOf(IllegalStateException.class);

            assertThat(calls.get()).isEqualTo(1);
        }
    }
}
