package site.explorationservice.recommendation.infrastructure;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LLM 호출을 포함하는 가중치 재계산 작업 전용 비동기 스레드풀.
 */
@Configuration
public class InterestWeightRecomputeConfig {

    @Bean
    public Executor interestWeightRecomputeExecutor() {
        return Executors.newFixedThreadPool(4);
    }
}
