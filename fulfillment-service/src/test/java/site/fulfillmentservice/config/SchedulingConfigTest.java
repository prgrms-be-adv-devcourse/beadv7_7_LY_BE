package site.fulfillmentservice.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@DisplayName("SchedulingConfig")
class SchedulingConfigTest {

    private AnnotationConfigApplicationContext context;

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Component
    static class BlockingOutboxRelayStandIn {
        final CountDownLatch started = new CountDownLatch(1);
        volatile String threadName;

        @Scheduled(scheduler = "outboxRelayScheduler", fixedDelay = Long.MAX_VALUE)
        public void relay() throws InterruptedException {
            threadName = Thread.currentThread().getName();
            started.countDown();
            Thread.sleep(3_000);
        }
    }

    @Component
    static class QuickDefaultSchedulerStandIn {
        final AtomicBoolean completed = new AtomicBoolean(false);
        volatile String threadName;

        @Scheduled(fixedDelay = Long.MAX_VALUE)
        public void quickTask() {
            threadName = Thread.currentThread().getName();
            completed.set(true);
        }
    }

    @Test
    @DisplayName("outboxRelayScheduler로 지정된 작업이 오래 걸려도, scheduler를 지정하지 않은 작업은 별도 스레드에서 영향받지 않고 돈다")
    void outboxRelayScheduler가_블로킹돼도_기본_스케줄러_작업은_영향받지_않는다() throws InterruptedException {
        // given
        context = new AnnotationConfigApplicationContext();
        context.register(SchedulingConfig.class, BlockingOutboxRelayStandIn.class, QuickDefaultSchedulerStandIn.class);
        context.refresh();

        BlockingOutboxRelayStandIn blocking = context.getBean(BlockingOutboxRelayStandIn.class);
        QuickDefaultSchedulerStandIn quick = context.getBean(QuickDefaultSchedulerStandIn.class);

        // when: outboxRelayScheduler가 3초짜리 블로킹 작업을 시작할 때까지 기다린다
        assertThat(blocking.started.await(2, TimeUnit.SECONDS)).isTrue();

        // then: 기본 스케줄러의 짧은 작업은 블로킹이 끝나길 기다리지 않고 곧바로 완료된다
        await().atMost(Duration.ofSeconds(2)).untilTrue(quick.completed);
        assertThat(blocking.threadName).startsWith("outbox-relay-");
        assertThat(quick.threadName).startsWith("scheduling-");
    }
}
