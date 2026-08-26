package site.pointwalletservice.hold.application;
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
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import site.pointwalletservice.hold.exception.HoldLockContentionException;
import site.pointwalletservice.hold.infrastructure.HoldJpaRepository;
import site.pointwalletservice.hold.infrastructure.HoldRepositoryImpl;
import site.pointwalletservice.ledger.application.PointTransactionApplicationService;
import site.pointwalletservice.ledger.infrastructure.PointTransactionRepositoryImpl;
import site.pointwalletservice.shared.Money;
import site.pointwalletservice.wallet.application.WalletApplicationService;
import site.pointwalletservice.wallet.domain.Wallet;
import site.pointwalletservice.wallet.infrastructure.WalletJpaRepository;
import site.pointwalletservice.wallet.infrastructure.WalletRepositoryImpl;

/**
 * HoldNormalScenarioPerformanceTest(같은 패키지)와 원래 한 파일이었는데, 실행 시간이 너무 길어서
 * (동시성 6종 x jitter 4종 x 반복까지 합치면 40회 이상) 분리했다 - 스트레스 구간(동시성 10~20)만
 * 여기서 본다. 시나리오/측정 방식 등 자세한 설명은 HoldNormalScenarioPerformanceTest 클래스
 * 주석 참고.
 * <p>
 * 동시성 상한을 20으로 잡은 이유 - pointwallet-service의 HikariCP 커넥션 풀은
 * application-local.yml에도 별도 설정이 없어 스프링부트 기본값(10)을 그대로 쓴다. 즉 실제
 * 운영 환경에서 이 서비스가 동시에 처리할 수 있는 DB 커넥션이 물리적으로 10개뿐이라, 그 2배인
 * 20을 "환경이 허용하는 최악의 부하"로 보고 상한을 잡았다(원래는 25/30까지 돌려봤는데, 풀
 * 크기를 초과하는 동시성에서 커넥션 경합 자체가 테스트를 불안정하게 만들어서 - 실제 서비스
 * 로직 문제가 아니라 테스트 인프라 한계라 스코프에서 뺐다).
 * <p>
 * 실행 전: docker compose -f docker/local/docker-compose.yml up -d mysql
 * 평소 빌드에는 안 끼도록 @Disabled로 막아뒀다 - 지울 때(또는 --tests로 직접 지정할 때만) 켜서 쓴다.
 * Gradle이 UP-TO-DATE로 캐시된 이전 결과를 재사용해 실제로 안 도는 경우가 있으니, 재실행할 땐
 * --rerun-tasks를 꼭 같이 준다.
 */
@DataJpaTest
@ActiveProfiles("perf")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        WalletRepositoryImpl.class, WalletApplicationService.class,
        PointTransactionRepositoryImpl.class, PointTransactionApplicationService.class,
        HoldRepositoryImpl.class, HoldApplicationService.class
})
@Disabled("로컬 MySQL 필요 - docker compose up -d mysql 이후 수동으로만 실행")
class HoldStressPerformanceTest {

    @Autowired
    private WalletJpaRepository walletJpaRepository;

    @Autowired
    private HoldJpaRepository holdJpaRepository;

    @Autowired
    private HoldApplicationService holdApplicationService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    // RetryingHoldService.hold()의 @Retryable과 동일한 정책.
    private static final int MAX_RETRIES = 5;
    private static final long BASE_DELAY_MS = 50;
    private static final double MULTIPLIER = 2;
    private static final long MAX_DELAY_MS = 800;

    private static final long[] JITTER_CANDIDATES_MS = {0, 10, 25, 50};

    // 0/10은 이미 "구조적으로 안 좋다"가 확인됐으니 1회만, 실제 후보인 25/50은 노이즈(스레드
    // 스케줄링 변동) 영향을 줄이려고 여러 번 돌려서 평균/분산을 본다.
    private static final int REPEAT_COUNT_FOR_CANDIDATES = 3;

    private static final Money HOLD_AMOUNT = Money.of(1_000);

    @ParameterizedTest(name = "동시성 {0}")
    @ValueSource(ints = {10, 15, 18, 20})
    void 한_유저가_여러_경매에_동시_입찰할_때_hold_전체_왕복시간을_지터별로_측정한다(int concurrency) throws Exception {
        for (long jitterMs : JITTER_CANDIDATES_MS) {
            int repeats = (jitterMs == 25 || jitterMs == 50) ? REPEAT_COUNT_FOR_CANDIDATES : 1;
            for (int repeat = 0; repeat < repeats; repeat++) {
                runOnce(concurrency, jitterMs, repeat);
            }
        }
    }

    private void runOnce(int concurrency, long jitterMs, int repeatIndex) throws Exception {
        Long userId = 800_000L + concurrency * 10_000L + jitterMs * 10 + repeatIndex;

        TransactionTemplate requiresNew = new TransactionTemplate(transactionManager);
        requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        // 재실행 시 이전 실행분이 남아있어도(Gradle 데몬이 컨텍스트를 캐싱해서 create-drop이
        // 매번 새로 안 도는 경우가 있음) 안전하게 다시 돌 수 있도록, 있으면 지우고 새로 만든다.
        requiresNew.executeWithoutResult(status ->
                walletJpaRepository.findByUserId(userId).ifPresent(walletJpaRepository::delete));
        requiresNew.executeWithoutResult(status -> {
            Wallet wallet = Wallet.open(userId);
            wallet.charge(Money.of(concurrency * 2_000L));
            walletJpaRepository.save(wallet);
        });
        // 이번 실행에서 쓸 auctionId들에 이전 실행분 Hold가 남아있으면 hold()가 "기존 홀드가
        // 있다"고 오판해 엉뚱한 지갑을 건드릴 수 있어, 마찬가지로 미리 지운다.
        for (int i = 0; i < concurrency; i++) {
            Long auctionId = userId * 1000 + i;
            requiresNew.executeWithoutResult(status ->
                    holdJpaRepository.findByAuctionId(auctionId).ifPresent(holdJpaRepository::delete));
        }

        List<Long> latenciesMillis = new CopyOnWriteArrayList<>();
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger totalRetryCount = new AtomicInteger();
        AtomicInteger exhaustedCount = new AtomicInteger();
        CountDownLatch allStart = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        for (int i = 0; i < concurrency; i++) {
            Long auctionId = userId * 1000 + i;
            executor.submit(() -> {
                awaitQuietly(allStart);
                long startedAt = System.nanoTime();
                int attempt = 0;
                while (attempt <= MAX_RETRIES) {
                    try {
                        requiresNew.executeWithoutResult(status ->
                                holdApplicationService.hold(auctionId, userId, HOLD_AMOUNT));
                        latenciesMillis.add((System.nanoTime() - startedAt) / 1_000_000);
                        successCount.incrementAndGet();
                        totalRetryCount.addAndGet(attempt);
                        return;
                    } catch (HoldLockContentionException e) {
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
        System.out.println("===== hold() 전체 왕복시간 측정 (동시성 " + concurrency + " / jitter " + jitterMs
                + "ms / repeat " + repeatIndex + ") =====");
        System.out.println("전체 처리 시간(ms): " + totalElapsedMillis);
        System.out.println("성공: " + successCount.get() + " / 재시도 소진 실패: " + exhaustedCount.get()
                + " (실패율 " + String.format("%.1f", exhaustedCount.get() * 100.0 / concurrency) + "%)");
        System.out.println("총 재시도 횟수: " + totalRetryCount.get());
        System.out.println("p50(ms): " + percentile(sorted, 50));
        System.out.println("p95(ms): " + percentile(sorted, 95));
        System.out.println("max(ms): " + sorted.get(sorted.size() - 1));
    }

    // RetryingHoldService의 @Retryable과 동일한 jitter 의미(=jitter도 multiplier 적용, base delay
    // 밑/maxDelay 위로는 clamp)를 재현한다.
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