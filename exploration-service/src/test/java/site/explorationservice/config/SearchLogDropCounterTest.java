package site.explorationservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("검색 로그 유실 카운터")
class SearchLogDropCounterTest {

    @Test
    @DisplayName("처음에는 버린 건수가 0이다")
    void 초기값은_0() {
        // given
        final SearchLogDropCounter counter = new SearchLogDropCounter();

        // when
        final long dropped = counter.getDroppedCount();

        // then
        assertThat(dropped).isZero();
    }

    @Test
    @DisplayName("거절될 때마다 버린 건수가 하나씩 늘어난다")
    void 거절되면_건수가_는다() {
        // given
        final SearchLogDropCounter counter = new SearchLogDropCounter();
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(1));

        // when
        counter.rejectedExecution(() -> {
        }, executor);
        counter.rejectedExecution(() -> {
        }, executor);

        // then
        assertThat(counter.getDroppedCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("버려도 예외를 던지지 않는다")
    void 버려도_예외를_안_던진다() {
        // given
        final SearchLogDropCounter counter = new SearchLogDropCounter();
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(1));

        // when
        counter.rejectedExecution(() -> {
        }, executor);

        // then
        assertThat(counter.getDroppedCount()).isEqualTo(1L);
    }
}
