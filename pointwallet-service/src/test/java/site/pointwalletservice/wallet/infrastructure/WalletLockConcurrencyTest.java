package site.pointwalletservice.wallet.infrastructure;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import site.pointwalletservice.wallet.domain.Wallet;
import site.pointwalletservice.support.RepositoryTest;

/**
 * Mockito 단위테스트는 리포지토리가 mock이라 SQL 자체가 안 나가서 락이 실제로 걸리는지 검증할 수 없다는
 * 리뷰 코멘트(chanyong1027)에 대응하는 통합테스트 - 스레드 두 개로 같은 지갑에 동시에 락을 걸어본다.
 *
 * 지갑 락은 홀드 행 락(NOWAIT)과 다르게 기다린다 - 같은 유저가 서로 다른 두 경매에 동시에 입찰하는 것도
 * 정책상 정상이라, 지갑 레벨 경합은 "누가 이기냐"가 아니라 순서대로 처리하면 둘 다 성공해야 하는
 * 상황이기 때문이다. 그래서 이 테스트는 실패가 아니라 "기다렸다가 결국 둘 다 성공하는지"를 검증한다.
 *
 * 주의: 테스트 DB는 H2(MODE=MySQL)라 SQL 문법만 MySQL 호환이고, Hibernate가 실제로 쓰는 다이얼렉트는
 * H2Dialect다(MySQLDialect 아님) - 비관적 락이 실제로 걸리고 경합 시 대기한다는 동작 자체는 검증되지만,
 * MySQL 운영 환경의 정확한 대기시간(innodb_lock_wait_timeout)까지 이 테스트가 보장하진 않는다.
 */
@RepositoryTest
class WalletLockConcurrencyTest {

    @Autowired
    private WalletJpaRepository walletJpaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate requiresNew;

    @BeforeEach
    void setUp() {
        requiresNew = new TransactionTemplate(transactionManager);
        requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Test
    @DisplayName("한 스레드가 락을 쥐고 있는 동안 다른 스레드가 같은 지갑을 잠그려 하면 실패하지 않고 기다렸다가 성공한다")
    void 같은_지갑에_동시에_락을_걸면_기다렸다가_둘_다_성공한다() throws Exception {
        // given: 다른 스레드에서도 보여야 하니 REQUIRES_NEW로 별도 트랜잭션에 커밋해둔다.
        Long userId = 777L;
        requiresNew.executeWithoutResult(status -> walletJpaRepository.save(Wallet.open(userId)));

        long holdMillis = 500L; // 첫 번째 스레드가 락을 쥐고 있는 시간
        CountDownLatch firstThreadHoldsLock = new CountDownLatch(1);
        CountDownLatch secondThreadStartedWaiting = new CountDownLatch(1);
        AtomicReference<Throwable> secondThreadError = new AtomicReference<>();
        AtomicLong secondThreadElapsedMillis = new AtomicLong();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            // 첫 번째 스레드: 락을 잡고, 두 번째 스레드가 대기를 시작한 뒤에도 holdMillis만큼 더 쥐고 있는다.
            Future<?> first = executor.submit(() ->
                    requiresNew.executeWithoutResult(status -> {
                        walletJpaRepository.findByUserIdForUpdate(userId);
                        firstThreadHoldsLock.countDown();
                        awaitQuietly(secondThreadStartedWaiting, 5);
                        sleepQuietly(holdMillis);
                    })
            );

            // 두 번째 스레드: 첫 번째가 락을 잡을 때까지 기다렸다가 같은 지갑에 락을 시도한다 - 이때 블로킹돼야 한다.
            Future<?> second = executor.submit(() -> {
                awaitQuietly(firstThreadHoldsLock, 5);
                long startedAt = System.nanoTime();
                secondThreadStartedWaiting.countDown();
                try {
                    requiresNew.executeWithoutResult(status ->
                            walletJpaRepository.findByUserIdForUpdate(userId));
                } catch (Throwable t) {
                    secondThreadError.set(t);
                } finally {
                    secondThreadElapsedMillis.set((System.nanoTime() - startedAt) / 1_000_000);
                }
            });

            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdown();
        }

        // then: 예외 없이 성공해야 하고, 첫 번째가 락을 놓아줄 때까지 실제로 기다렸어야 한다
        // (즉시 실패했다면 걸린 시간이 holdMillis보다 훨씬 짧게 나온다).
        assertThat(secondThreadError.get()).isNull();
        assertThat(secondThreadElapsedMillis.get()).isGreaterThanOrEqualTo(holdMillis - 100);
    }

    private void awaitQuietly(CountDownLatch latch, long timeoutSeconds) {
        try {
            latch.await(timeoutSeconds, TimeUnit.SECONDS);
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