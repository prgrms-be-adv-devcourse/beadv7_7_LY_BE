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
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import site.pointwalletservice.wallet.domain.Wallet;
import site.pointwalletservice.support.RepositoryTest;

/**
 * Mockito 단위테스트는 리포지토리가 mock이라 SQL 자체가 안 나가서 락이 실제로 걸리는지 검증할 수 없다는
 * 리뷰 코멘트(chanyong1027)에 대응하는 통합테스트 - 스레드 두 개로 같은 지갑에 동시에 락을 걸어본다.
 *
 * 지갑 락은 홀드 행 락과 동일하게 NOWAIT이다 - 경합 시 기다리지 않고 즉시 실패한다. "같은 유저의 동시
 * 입찰"처럼 실패로 이어지면 안 되는 경합은 RetryingHoldService가 hold() 호출 자체를 바깥에서 재시도하는
 * 방식으로 흡수하므로, 이 레이어에서는 "기다렸다가 성공"이 아니라 "즉시 실패"를 검증하는 게 맞다.
 *
 * 주의: 테스트 DB는 H2(MODE=MySQL)라 SQL 문법만 MySQL 호환이고, Hibernate가 실제로 쓰는 다이얼렉트는
 * H2Dialect다(MySQLDialect 아님) - jakarta.persistence.lock.timeout=0 힌트가 실제로 즉시 실패로
 * 이어지는지는 검증되지만, MySQL 운영 환경과 완전히 동일한 에러 코드/메시지까지 보장하진 않는다.
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
    @DisplayName("한 스레드가 락을 쥐고 있는 동안 다른 스레드가 같은 지갑을 잠그려 하면 기다리지 않고 즉시 실패한다")
    void 같은_지갑에_동시에_락을_걸면_기다리지_않고_즉시_실패한다() throws Exception {
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
            // 첫 번째 스레드: 락을 잡고, 두 번째 스레드가 시도를 시작한 뒤에도 holdMillis만큼 더 쥐고 있는다.
            Future<?> first = executor.submit(() ->
                    requiresNew.executeWithoutResult(status -> {
                        walletJpaRepository.findByUserIdForUpdate(userId);
                        firstThreadHoldsLock.countDown();
                        awaitQuietly(secondThreadStartedWaiting, 5);
                        sleepQuietly(holdMillis);
                    })
            );

            // 두 번째 스레드: 첫 번째가 락을 잡을 때까지 기다렸다가 같은 지갑에 락을 시도한다 - NOWAIT이라
            // 기다리지 않고 곧바로 실패해야 한다.
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

        // then: 첫 번째가 락을 놓아줄 때까지 기다리지 않고 즉시 락 획득 실패 예외를 받았어야 한다
        // (실제로 기다렸다면 걸린 시간이 holdMillis에 가깝게 나온다).
        assertThat(secondThreadError.get()).isInstanceOf(PessimisticLockingFailureException.class);
        assertThat(secondThreadElapsedMillis.get()).isLessThan(holdMillis / 2);
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