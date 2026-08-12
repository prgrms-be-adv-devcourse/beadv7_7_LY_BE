package site.pointwalletservice.wallet.infrastructure;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import site.pointwalletservice.wallet.domain.Wallet;

/**
 * 로컬 실제 MySQL(docker/local/docker-compose.yml의 mysql)에 붙여서 지갑 락 NOWAIT+재시도의
 * 실제 지연시간/실패율을 동시성 값 x 지터 값 조합별로 측정한다. H2가 아니라 진짜 MySQL InnoDB 락 동작을 봐야
 * 의미가 있어서 일반 유닛/통합테스트(WalletLockConcurrencyTest, H2)와 분리해뒀다.
 * <p>
 * JITTER_CANDIDATES_MS: 0(기준선), 10(현재 RetryingHoldService 적용값),
 * 25/50(1차 라운드 폭이 HOLD_MILLIS=50ms를 넘어서는 지점)을 같이 비교해서 10이 최적인지 확인한다.
 * delayWithJitter()는 Spring @Retryable의 jitter 의미(=jitter도 multiplier 적용, base delay 밑/
 * maxDelay 위로는 clamp)를 그대로 재현한다.
 * <p>
 * 실행 전: docker compose -f docker/local/docker-compose.yml up -d mysql
 * 평소 빌드에는 안 끼도록 @Disabled로 막아뒀다 - 지울 때(또는 --tests로 직접 지정할 때만) 켜서 쓴다.
 */
@DataJpaTest
@ActiveProfiles("perf")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Disabled("로컬 MySQL 필요 - docker compose up -d mysql 이후 수동으로만 실행")
class WalletLockPerformanceTest {

    @Autowired
    private WalletJpaRepository walletJpaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    // RetryingHoldService(hold/application)의 실제 정책과 동일하게 맞춘다.
    private static final int MAX_RETRIES = 5;
    private static final long BASE_DELAY_MS = 50;
    private static final double MULTIPLIER = 2;
    private static final long MAX_DELAY_MS = 800;

    // 비교할 지터 후보 - 0(기준선), 10(현재 적용값), 25/50(1차 라운드 폭이 HOLD_MILLIS=50ms를 넘어서는 지점)
    private static final long[] JITTER_CANDIDATES_MS = {0, 10, 25, 50};

    // 동시성 값마다, 지터 값마다 다른 지갑을 쓰도록 조합 자체를 userId 베이스로 삼는다
    // (재실행/여러 조합 연속 실행 시 order_id/user_id 유니크 제약 충돌 방지).
    @ParameterizedTest(name = "동시성 {0}")
    @ValueSource(ints = {10, 15, 18, 20, 25, 30}) // 무지터 실측에서 100% 성공했던 5는 제외, 문제구간만
    void 같은_지갑에_동시_요청_N개가_몰릴_때_지터별_지연시간을_측정한다(int concurrency) throws Exception {
        for (long jitterMs : JITTER_CANDIDATES_MS) {
            runOnce(concurrency, jitterMs);
        }
    }

    private void runOnce(int concurrency, long jitterMs) throws Exception {
        Long userId = 900_000L + concurrency * 1000L + jitterMs;

        TransactionTemplate requiresNew = new TransactionTemplate(transactionManager);
        requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        requiresNew.executeWithoutResult(status -> walletJpaRepository.save(Wallet.open(userId)));

        List<Long> latenciesMillis = new CopyOnWriteArrayList<>();
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger totalRetryCount = new AtomicInteger();
        AtomicInteger exhaustedCount = new AtomicInteger();
        CountDownLatch allStart = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        for (int i = 0; i < concurrency; i++) {
            executor.submit(() -> {
                awaitQuietly(allStart);
                long startedAt = System.nanoTime();
                int attempt = 0;
                while (attempt <= MAX_RETRIES) {
                    try {
                        requiresNew.executeWithoutResult(status -> {
                            walletJpaRepository.findByUserIdForUpdate(userId);
                            sleepQuietly(50); // 실제 hold() 처리 시간(조회~저장)을 흉내낸 지연
                        });
                        latenciesMillis.add((System.nanoTime() - startedAt) / 1_000_000);
                        successCount.incrementAndGet();
                        totalRetryCount.addAndGet(attempt);
                        return;
                    } catch (PessimisticLockingFailureException e) {
                        attempt++;
                        if (attempt > MAX_RETRIES) {
                            exhaustedCount.incrementAndGet();
                            latenciesMillis.add((System.nanoTime() - startedAt) / 1_000_000);
                            return;
                        }
                        sleepQuietly(delayWithJitter(attempt, jitterMs));
                    }
                }
            });
        }

        long testStartedAt = System.nanoTime();
        allStart.countDown();
        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);
        long totalElapsedMillis = (System.nanoTime() - testStartedAt) / 1_000_000;

        List<Long> sorted = latenciesMillis.stream().sorted().toList();
        System.out.println("===== 지갑 락 NOWAIT+재시도 성능 측정 (동시성 " + concurrency + " / jitter " + jitterMs + "ms) =====");
        System.out.println("전체 처리 시간(ms): " + totalElapsedMillis);
        System.out.println("성공: " + successCount.get() + " / 재시도 소진 실패: " + exhaustedCount.get()
                + " (실패율 " + String.format("%.1f", exhaustedCount.get() * 100.0 / concurrency) + "%)");
        System.out.println("총 재시도 횟수: " + totalRetryCount.get());
        System.out.println("p50(ms): " + percentile(sorted, 50));
        System.out.println("p95(ms): " + percentile(sorted, 95));
        System.out.println("max(ms): " + sorted.get(sorted.size() - 1));
    }

    // Spring @Retryable의 jitter 의미와 동일하게 재현한다: jitter도 delay와 같은 multiplier를 적용받고,
    // 결과값은 base delay 밑/maxDelay 위로는 나가지 않게 clamp된다.
    private long delayWithJitter(int attempt, long jitterMs) {
        long calculatedDelay = (long) (BASE_DELAY_MS * Math.pow(MULTIPLIER, attempt - 1));
        long scaledJitter = (long) (jitterMs * Math.pow(MULTIPLIER, attempt - 1));
        long randomOffset = scaledJitter == 0 ? 0
                : ThreadLocalRandom.current().nextLong(-scaledJitter, scaledJitter + 1);
        long delay = calculatedDelay + randomOffset;
        return Math.max(BASE_DELAY_MS, Math.min(delay, MAX_DELAY_MS));
    }

    private long percentile(List<Long> sorted, int p) {
        int index = Math.min(sorted.size() - 1, (int) Math.ceil(sorted.size() * (p / 100.0)) - 1);
        return sorted.get(Math.max(0, index));
    }

    private void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}