package site.explorationservice.config;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 대기 큐가 가득 찼을 때 저장 작업을 버리고 그 수를 센다.
 * <p>
 * 기본 정책은 예외를 던지는데, 그 예외는 검색을 호출한 스레드로 올라간다. 기록을 남기려다 검색이 실패하는
 * 것은 앞뒤가 바뀐 상황이라 여기서 조용히 버린다.
 * <p>
 * 대신 버린 수를 남긴다. 아무 흔적 없이 버리면 나중에 통계를 볼 때 "검색이 줄었다"와 "기록을 놓쳤다"를
 * 구분할 수 없다.
 */
@Slf4j
@Component
public class SearchLogDropCounter implements RejectedExecutionHandler {

    private final AtomicLong dropped = new AtomicLong();

    @Override
    public void rejectedExecution(final Runnable runnable, final ThreadPoolExecutor executor) {
        final long total = dropped.incrementAndGet();
        log.warn("검색 로그 저장 작업을 버렸습니다 — 누적 {}건. 대기 큐가 가득 찼습니다", total);
    }

    public long getDroppedCount() {
        return dropped.get();
    }
}
