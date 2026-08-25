package site.fulfillmentservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableScheduling
public class SchedulingConfig {

    /**
     * TaskScheduler 빈이 하나라도 있으면 Boot가 기본 스케줄러를 자동 생성하지 않으므로,
     * outboxRelayScheduler 추가 후엔 이것도 직접 정의해야 한다. scheduler를 지정하지 않은
     * @Scheduled 메서드들이 여기로 모이도록 @Primary로 명시한다.
     */
    @Primary
    @Bean
    public TaskScheduler defaultTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("scheduling-");
        scheduler.initialize();
        return scheduler;
    }

    /**
     * OutboxRelay 전용 스케줄러. Kafka 발행이 블로킹되면 이 스레드만 묶이고,
     * defaultTaskScheduler를 쓰는 나머지 스케줄러는 영향받지 않는다.
     */
    @Bean
    public TaskScheduler outboxRelayScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("outbox-relay-");
        scheduler.initialize();
        return scheduler;
    }
}
