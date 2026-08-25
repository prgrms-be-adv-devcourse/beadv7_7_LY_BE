package site.explorationservice.recommendation.infrastructure;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LLM 호출을 포함하는 가중치 재계산 작업 전용 비동기 스레드풀.
 */
@Configuration
public class InterestWeightRecomputeConfig {

    private static final int CORE_POOL_SIZE = 4;
    private static final int QUEUE_CAPACITY = 200;

    @Bean
    public Executor interestWeightRecomputeExecutor() {
        return new ThreadPoolExecutor(CORE_POOL_SIZE, CORE_POOL_SIZE, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(QUEUE_CAPACITY));
    }
}
