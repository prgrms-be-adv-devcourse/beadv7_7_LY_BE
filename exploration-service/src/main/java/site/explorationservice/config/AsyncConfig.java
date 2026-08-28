package site.explorationservice.config;

import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 검색 로그 저장 전용 스레드 풀.
 * <p>
 * 기본 스레드 풀을 그대로 쓰지 않는 이유는 대기 큐 크기에 제한이 없기 때문이다. 저장이 느려지면 처리하지 못한
 * 작업이 메모리에 무한정 쌓여서, 유실돼도 되는 기록 때문에 서버가 위험해진다.
 * <p>
 * 크기는 트래픽 가정에서 나왔다. 검색이 초당 80건까지 들어온다고 보고 한 건을 저장하는 데 50밀리초가
 * 걸린다고 하면 4개가 필요하다.
 * <p>
 * 최대 스레드는 큐가 가득 찬 뒤에야 늘어난다. 자바 스레드 풀이 "코어 스레드 → 큐 → 최대 스레드" 순서로
 * 채우기 때문인데, 그래서 이 값은 평소에 쓰이지 않고 마지막 방어선으로만 동작한다.
 * <p>
 * 종료할 때 남은 작업을 기다린다. 배포는 자주 일어나는데 그때마다 큐에 있던 기록이 통째로 사라지는 것은
 * 설정 두 줄로 막을 수 있다.
 */
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class AsyncConfig {

    private static final int CORE_POOL_SIZE = 2;
    private static final int MAX_POOL_SIZE = 4;
    private static final int QUEUE_CAPACITY = 1_000;
    private static final int AWAIT_TERMINATION_SECONDS = 10;

    private final SearchLogDropCounter searchLogDropCounter;

    @Bean
    public Executor searchLogExecutor() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix("search-log-");
        executor.setRejectedExecutionHandler(searchLogDropCounter);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(AWAIT_TERMINATION_SECONDS);
        executor.initialize();
        return executor;
    }
}
