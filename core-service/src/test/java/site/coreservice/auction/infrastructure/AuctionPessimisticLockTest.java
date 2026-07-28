package site.coreservice.auction.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import site.coreservice.auction.domain.Auction;
import site.coreservice.auction.domain.AuctionRepository;
import site.coreservice.auction.domain.AuctionSchedule;
import site.coreservice.auction.domain.AuctionStatus;
import site.coreservice.auction.domain.ItemCondition;
import site.coreservice.auction.domain.ItemInfo;
import site.coreservice.auction.domain.Money;
import site.coreservice.auction.domain.Period;
import site.coreservice.auction.domain.Pricing;
import site.coreservice.support.RepositoryTest;

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
            auctionRepository.findByIdForUpdate(auctionId);
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
                auctionRepository.findByIdForUpdate(auctionId);
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
    @DisplayName("락 대기 시간이 타임아웃(3초)을 넘으면 예외를 던진다")
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
            auctionRepository.findByIdForUpdate(auctionId);
            aAcquired.countDown();
            await(releaseA); // 타임아웃(3초)보다 오래 락을 쥐고 있는다 - B는 그 전에 실패해야 한다
        }));

        Thread threadB = new Thread(() -> {
            await(aAcquired);
            try {
                txTemplate.executeWithoutResult(status -> auctionRepository.findByIdForUpdate(auctionId));
            } catch (Exception e) {
                bFailure.set(e);
            }
        });

        long start = System.currentTimeMillis();
        threadA.start();
        threadB.start();
        threadB.join(10_000); // 타임아웃(3초)만큼 대기했다가 예외를 던지고 끝날 때까지 대기
        long elapsedMillis = System.currentTimeMillis() - start;

        releaseA.countDown(); // A는 정리 차원에서 커밋 진행
        threadA.join(5_000);

        // then: B는 무한정 기다리지 않고 타임아웃 근처(3초)에서 실패해야 한다
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
