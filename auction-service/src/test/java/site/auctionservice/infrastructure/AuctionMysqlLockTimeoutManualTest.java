package site.auctionservice.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import site.auctionservice.domain.*;
import site.auctionservice.exception.AuctionErrorCode;
import site.auctionservice.exception.AuctionException;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * docker-compose 로 띄운 실제 MySQL 컨테이너(local 프로파일, localhost:33340)에 붙어
 * AuctionRepositoryImpl.findByIdForUpdate()가 실제로 3초 뒤 락 타임아웃으로 실패하는지
 * 검증하기 위한 로컬 전용 테스트. mysql 컨테이너가 떠있지 않으면 실패하므로 CI에서는
 * 절대 돌면 안 된다 - 그래서 @Disabled로 꺼둔다. 로컬에서 재검증하고 싶으면 mysql
 * 컨테이너를 띄운 뒤 아래 @Disabled를 지우고 실행한 다음, 다시 붙여서 커밋한다.
 */
@DataJpaTest
@ActiveProfiles("local")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AuctionRepositoryImpl.class)
class AuctionMysqlLockTimeoutManualTest {

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private AuctionJpaRepository auctionJpaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @Disabled("로컬 mysql 컨테이너 필요 - docker/local docker-compose로 mysql 띄운 뒤에만 지우고 실행")
    @DisplayName("실제 MySQL에서 findByIdForUpdate()는 락 대기 3초를 넘기면 AuctionException(LOCK_ACQUISITION_FAILED)을 던진다")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void findByIdForUpdate_realMysql_timesOutAfterThreeSeconds() throws InterruptedException {
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
        AtomicLong bElapsedMillis = new AtomicLong(-1);

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        Thread threadA = new Thread(() -> txTemplate.executeWithoutResult(status -> {
            auctionJpaRepository.findByIdForUpdate(auctionId);
            aAcquired.countDown();
            await(releaseA); // 3초보다 오래 락을 쥐고 있는다 - B는 그 전에 타임아웃으로 실패해야 한다
        }));

        Thread threadB = new Thread(() -> {
            await(aAcquired);
            long start = System.currentTimeMillis();
            try {
                // 프로덕션 경로 그대로: setLockWaitTimeout(3) -> findByIdForUpdate -> 예외 번역
                txTemplate.executeWithoutResult(status -> auctionRepository.findByIdForUpdate(auctionId));
            } catch (Exception e) {
                bFailure.set(e);
            } finally {
                bElapsedMillis.set(System.currentTimeMillis() - start);
            }
        });

        threadA.start();
        threadB.start();
        threadB.join(10_000);

        releaseA.countDown(); // A는 정리 차원에서 커밋 진행
        threadA.join(5_000);

        assertThat(bFailure.get())
                .as("B는 3초 락 대기 후 예외를 던져야 한다")
                .isInstanceOf(AuctionException.class);
        assertThat(((AuctionException) bFailure.get()).getErrorCode())
                .isEqualTo(AuctionErrorCode.LOCK_ACQUISITION_FAILED);
        // 3초 타임아웃 + 오버헤드를 감안한 범위 (너무 빨리 실패해도, 너무 오래 걸려도 안 된다)
        assertThat(bElapsedMillis.get()).isBetween(2_500L, 5_000L);
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
            ItemInfo itemInfo = ItemInfo.of(ItemCondition.MINT, "MySQL 락 타임아웃 검증용 상품 설명입니다.", null);
            Pricing pricing = Pricing.of(Money.of(1_000L), Money.of(100L), Money.of(0L));
            AuctionSchedule schedule = AuctionSchedule.of(
                    Period.of(LocalDateTime.now(), LocalDateTime.now().plusDays(1)), false, null);
            Auction auction = auctionRepository.save(
                    Auction.of(1L, 900_000L, itemInfo, pricing, schedule, AuctionStatus.RUNNING, null));
            return auction.getId();
        });
    }
}
