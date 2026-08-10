package site.pointwalletservice.wallet.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
 * 주의: 테스트 DB는 H2(MODE=MySQL)라 SQL 문법만 MySQL 호환이고, Hibernate가 실제로 쓰는 다이얼렉트는
 * H2Dialect다(MySQLDialect 아님) - 즉 이 테스트는 "비관적 락이 실제로 걸리고 경합 시 실패한다"는 것만
 * 검증하고, MySQL 운영 환경에서 NOWAIT이 정확히 이 타이밍대로 동작하는지까지 보장하진 않는다.
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
    @DisplayName("한 스레드가 락을 쥐고 있는 동안 다른 스레드가 같은 지갑을 잠그려 하면 기다리지 않고 즉시 실패한다(NOWAIT)")
    void 같은_지갑에_동시에_락을_걸면_한쪽은_즉시_실패한다() throws Exception {
        // given: 다른 스레드에서도 보여야 하니 REQUIRES_NEW로 별도 트랜잭션에 커밋해둔다.
        Long userId = 777L;
        requiresNew.executeWithoutResult(status -> walletJpaRepository.save(Wallet.open(userId)));

        CountDownLatch firstThreadHoldsLock = new CountDownLatch(1);
        CountDownLatch secondThreadAttempted = new CountDownLatch(1);
        AtomicReference<Throwable> secondThreadError = new AtomicReference<>();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            // 첫 번째 스레드: 락을 잡고, 두 번째 스레드가 시도를 마칠 때까지 쥐고 있는다.
            Future<?> first = executor.submit(() ->
                    requiresNew.executeWithoutResult(status -> {
                        walletJpaRepository.findByUserIdForUpdate(userId);
                        firstThreadHoldsLock.countDown();
                        awaitQuietly(secondThreadAttempted);
                    })
            );

            // 두 번째 스레드: 첫 번째가 락을 잡을 때까지 기다렸다가 같은 지갑에 락을 시도한다.
            Future<?> second = executor.submit(() -> {
                awaitQuietly(firstThreadHoldsLock);
                try {
                    requiresNew.executeWithoutResult(status ->
                            walletJpaRepository.findByUserIdForUpdate(userId));
                } catch (Throwable t) {
                    secondThreadError.set(t);
                } finally {
                    secondThreadAttempted.countDown();
                }
            });

            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
        } finally {
            executor.shutdown();
        }

        // then: NOWAIT이므로 대기 없이 즉시 실패해야 한다.
        assertThat(secondThreadError.get())
                .isInstanceOf(PessimisticLockingFailureException.class);
    }

    private void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}