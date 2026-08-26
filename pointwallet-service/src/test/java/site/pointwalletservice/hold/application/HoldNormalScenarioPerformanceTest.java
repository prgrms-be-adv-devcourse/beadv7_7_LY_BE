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
 * HoldStressPerformanceTest(같은 패키지)와 원래 한 파일이었는데, 실행 시간이 너무 길어서 분리했다 -
 * "정상 시나리오"로 판단한 동시성 2~5만 여기서 본다.
 * <p>
 * WalletLockPerformanceTest(wallet/infrastructure)는 지갑 락 획득~해제 구간만 흉내내서 쟀다.
 * 이 테스트는 HoldApplicationService.hold() 전체(지갑락 대기+재시도 백오프 + deduct + Hold 저장 +
 * 원장 기록 전부)를 실제로 호출해서 잰다 - AuctionService.placeBid()가 경매 row 락을 쥔 채로
 * 동기 호출하는 게 바로 이 전체 구간이라, "경매 락 보유시간에 얼마나 얹히는가"는 지갑락 구간이
 * 아니라 이 전체 구간으로 봐야 정확하다.
 * <p>
 * RetryingHoldService(@Retryable, org.springframework.resilience)는 AOP 프록시 기반이라
 * @EnableResilientMethods가 켜진 실제 애플리케이션 컨텍스트에서만 동작한다 - @DataJpaTest는 그걸
 * 켜주지 않으므로, RetryingHoldServiceRetryPolicyTest(hold/application)와 같은 이유로 여기서도
 * HoldApplicationService를 직접 호출하면서 동일한 정책(maxRetries=5, delay=50, jitter=25,
 * multiplier=2, maxDelay=800)을 이 테스트 안에서 재현한다.
 * <p>
 * 시나리오는 wallet 락 벤치마크와 동일하게 "한 유저가 여러 경매에 동시 입찰"이다 - 스레드마다 다른
 * auctionId, 같은 userId를 써서 그 유저의 지갑 row에서만 경합이 나게 만든다(Hold 행 자체의 경합은
 * 대상이 아님 - auctionId가 스레드마다 달라서 HoldRowLockContentionException은 발생하지 않는다).
 * <p>
 * "한 유저가 여러 경매에 거의 동시에 입찰"하는 정상 시나리오의 상한으로 판단한 동시성 2~5 구간 -
 * 실제 프로덕션 정책(jitter=25)에서 지갑 락 경합이 얼마나 자주/얼마나 빨리 처리되는지 확인한다.
 * 스트레스 구간(HoldStressPerformanceTest)과 달리 여기서는 jitter 비교가 목적이 아니라 "평소엔
 * 문제없다"는 대비 수치를 확보하는 게 목적이라 jitter=25 하나만, 반복 없이 1회씩 본다.
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
class HoldNormalScenarioPerformanceTest {

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

    private static final long JITTER_MS = 25;

    private static final Money HOLD_AMOUNT = Money.of(1_000);

    @ParameterizedTest(name = "정상 시나리오 동시성 {0}")
    @ValueSource(ints = {2, 3, 4, 5})
    void 정상_시나리오_동시성에서_hold_전체_왕복시간을_측정한다(int concurrency) throws Exception {
        runOnce(concurrency, JITTER_MS, 0);
    }

    private void runOnce(int concurrency, long jitterMs, int repeatIndex) throws Exception {
        Long userId = 700_000L + concurrency * 10_000L + jitterMs * 10 + repeatIndex;

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
        System.out.println("===== hold() 전체 왕복시간 측정 (정상 시나리오 동시성 " + concurrency + " / jitter " + jitterMs
                + "ms) =====");
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