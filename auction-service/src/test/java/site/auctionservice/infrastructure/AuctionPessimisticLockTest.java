package site.auctionservice.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import site.auctionservice.domain.*;
import site.auctionservice.support.RepositoryTest;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * findByIdForUpdate()가 실제로 SELECT ... FOR UPDATE로 동작해
 * 동시 트랜잭션을 직렬화하는지 검증한다. AuctionServiceTest 등 Mockito 기반 테스트는
 * 리포지토리가 mock이라 SQL 자체가 안 나가므로, @Lock을 지워도 잡아내지 못한다.
 * 이 테스트는 실제 스레드 두 개 + 실제 트랜잭션 두 개로 락 경합을 재현한다.
 * <p>
 * 락은 AuctionJpaRepository(raw)를 직접 호출해서 건다 - AuctionRepositoryImpl.findByIdForUpdate를
 * 거치면 그 안에서 setLockWaitTimeout()(MySQL 전용 세션 변수)을 먼저 호출하는데, H2는 이 구문을 몰라서 SQLGrammarException이 난다
 * setLockWaitTimeout 호출 + 예외 번역 로직 자체는 AuctionRepositoryImplLockTest(Mockito)가 검증한다.
 */
@RepositoryTest
@Import(AuctionRepositoryImpl.class)
class AuctionPessimisticLockTest {

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private AuctionJpaRepository auctionJpaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("findByIdForUpdate()는 같은 auction row에 대해 동시 요청을 직렬화한다")
    @Transactional(propagation = Propagation.NOT_SUPPORTED) // 셋업이 실제로 커밋되어야 다른 스레드(다른 커넥션)에서 보인다
    void findByIdForUpdate_serializesConcurrentAccess() throws InterruptedException {
        // given: 실제로 커밋된 auction 한 건
        // 이 테스트는 @DataJpaTest의 롤백 대상이 아니라서(NOT_SUPPORTED), 끝나면 직접 지워야
        // 같은 프로세스에서 도는 다른 테스트(H2 인메모리 DB 공유)의 집계 결과를 오염시키지 않는다.
        Long auctionId = saveAuctionAndCommit();
        try {
            runConcurrencyScenario(auctionId);
        } finally {
            deleteAuctionAndCommit(auctionId);
        }
    }

    private void runConcurrencyScenario(Long auctionId) throws InterruptedException {
        List<String> events = Collections.synchronizedList(new java.util.ArrayList<>());
        CountDownLatch aAcquired = new CountDownLatch(1);
        CountDownLatch bStarted = new CountDownLatch(1);
        CountDownLatch releaseA = new CountDownLatch(1);

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        Thread threadA = new Thread(() -> txTemplate.executeWithoutResult(status -> {
            auctionJpaRepository.findByIdForUpdate(auctionId);
            events.add("A-acquired");
            aAcquired.countDown();
            await(releaseA); // 락을 쥔 채로 대기 - B가 확실히 블로킹되게 만든다
            events.add("A-committing");
        }));

        Thread threadB = new Thread(() -> {
            await(aAcquired); // A가 락을 잡은 뒤에만 시도한다
            bStarted.countDown();
            txTemplate.executeWithoutResult(status -> {
                // A가 커밋하기 전까지 이 호출 자체가 블로킹되어야 한다
                auctionJpaRepository.findByIdForUpdate(auctionId);
                events.add("B-acquired");
            });
        });

        threadA.start();
        threadB.start();
        assertThat(bStarted.await(2, TimeUnit.SECONDS)).isTrue();
        Thread.sleep(300); // B가 findByIdForUpdate 호출 안에서 실제로 대기 상태에 들어갈 시간을 준다
        releaseA.countDown(); // A 커밋 진행 -> B의 대기가 풀림

        threadA.join(5_000);
        threadB.join(5_000);

        // then: B는 A가 락을 잡고 있는 동안 절대 먼저 획득할 수 없다
        assertThat(events).containsExactly("A-acquired", "A-committing", "B-acquired");
    }

    @Test
    @DisplayName("락 대기가 길어지면 무한정 기다리지 않고 결국 예외를 던진다 (정확한 대기시간은 H2 기본값이며, 운영 MySQL의 3초 세션 변수와는 별개)")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void findByIdForUpdate_exceedsTimeout_throws() throws InterruptedException {
        Long auctionId = saveAuctionAndCommit();
        try {
            runTimeoutScenario(auctionId);
        } finally {
            deleteAuctionAndCommit(auctionId);
        }
    }

    private void runTimeoutScenario(Long auctionId) throws InterruptedException {
        CountDownLatch aAcquired = new CountDownLatch(1);
        CountDownLatch releaseA = new CountDownLatch(1);
        AtomicReference<Throwable> bFailure = new AtomicReference<>();

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        Thread threadA = new Thread(() -> txTemplate.executeWithoutResult(status -> {
            auctionJpaRepository.findByIdForUpdate(auctionId);
            aAcquired.countDown();
            await(releaseA); // H2 자체 기본 타임아웃보다 오래 락을 쥐고 있는다 - B는 그 전에 실패해야 한다
        }));

        Thread threadB = new Thread(() -> {
            await(aAcquired);
            try {
                // 주의: 여기서 setLockWaitTimeout()을 호출하지 않는다 - 이건 MySQL 전용 세션 변수
                // (SET innodb_lock_wait_timeout)라 H2에서는 SQLGrammarException(문법 오류,
                // SQLState 42001)이 난다. 호출해보면 이 catch가 "타임아웃 실패"가 아니라 그 문법
                // 오류를 잡아버려서, 실제로는 락 대기시간 제어를 전혀 검증하지 않은 채 테스트만
                // 통과하는 거짓 성공이 난다(WalletLockConcurrencyTest가 같은 이유로
                // setLockWaitTimeout을 호출하지 않는 것과 동일한 제약). 그래서 아래 타임아웃은
                // 우리 설정이 아니라 H2 자체의 기본 락 대기시간에서 비롯된다 - "언젠가는 실패한다"만
                // 검증할 수 있고 3초라는 구체적 수치는 이 테스트로 보장되지 않는다. 3초 세션 변수
                // 호출 + 예외 번역은 AuctionRepositoryImplLockTest(Mockito)가 검증한다.
                txTemplate.executeWithoutResult(status -> auctionJpaRepository.findByIdForUpdate(auctionId));
            } catch (Exception e) {
                bFailure.set(e);
            }
        });

        long start = System.currentTimeMillis();
        threadA.start();
        threadB.start();
        threadB.join(10_000); // H2 기본 락 대기시간만큼 대기했다가 예외를 던지고 끝날 때까지 대기
        long elapsedMillis = System.currentTimeMillis() - start;

        releaseA.countDown(); // A는 정리 차원에서 커밋 진행
        threadA.join(5_000);

        // then: B는 무한정 기다리지 않고 결국 실패해야 한다 (정확한 대기시간은 검증 대상 아님)
        assertThat(bFailure.get()).isNotNull();
        assertThat(elapsedMillis).isLessThan(9_000);
    }

    private void deleteAuctionAndCommit(Long auctionId) {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.executeWithoutResult(status -> auctionJpaRepository.deleteById(auctionId));
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private Long saveAuctionAndCommit() {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        return txTemplate.execute(status -> {
            ItemInfo itemInfo = ItemInfo.of(ItemCondition.MINT, "락 경합 테스트용 상품 설명입니다.", null);
            Pricing pricing = Pricing.of(Money.of(1_000L), Money.of(100L), Money.of(0L));
            AuctionSchedule schedule = AuctionSchedule.of(
                    Period.of(LocalDateTime.now(), LocalDateTime.now().plusDays(1)), false, null);
            Auction auction = auctionRepository.save(
                    Auction.of(1L, 900_000L, itemInfo, pricing, schedule, AuctionStatus.RUNNING, null));
            return auction.getId();
        });
    }
}
