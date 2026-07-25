package site.coreservice.auction.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.coreservice.auction.domain.Auction;
import site.coreservice.auction.domain.AuctionRepository;
import site.coreservice.auction.domain.AuctionSchedule;
import site.coreservice.auction.domain.AuctionStatus;
import site.coreservice.auction.domain.HighestBid;
import site.coreservice.auction.domain.ItemCondition;
import site.coreservice.auction.domain.ItemInfo;
import site.coreservice.auction.domain.Money;
import site.coreservice.auction.domain.Period;
import site.coreservice.auction.domain.Pricing;

import java.time.LocalDateTime;

/**
 * AuctionStartScheduler / AuctionCloseScheduler 수동 테스트용 시드 로더.
 * local 프로파일 + {@code auction.seed.enabled=true}일 때만 동작한다(기본 OFF).
 * 시작: 이미 지난 것/조기 시작 폭(5분) 이내/아직 먼 것.
 * 마감: 입찰 있고 이미 지난 것(낙찰)/입찰 없고 이미 지난 것(유찰)/아직 안 지난 것.
 * 멱등성: productId로 존재 여부를 확인하고 없을 때만 적재한다.
 */
@Slf4j
@Order(2)
@Profile("local")
@ConditionalOnProperty(prefix = "auction.seed", name = "enabled", havingValue = "true")
@Component
@RequiredArgsConstructor
public class AuctionSeedLoader implements CommandLineRunner {

    private static final Long SEED_SELLER_ID = 1L;

    private final AuctionRepository auctionRepository;
    private final AuctionJpaRepository auctionJpaRepository;

    @Override
    @Transactional
    public void run(String... args) {
        final LocalDateTime now = LocalDateTime.now();

        ensureScheduledAuction(1001L, now.minusMinutes(10)); // 이미 지남 -> 다음 폴링에 바로 시작
        ensureScheduledAuction(1002L, now.plusMinutes(3));   // 조기 시작 폭(5분) 이내 -> 다음 폴링에 시작
        ensureScheduledAuction(1003L, now.plusMinutes(10));  // 아직 멀었음 -> 시작 안 됨

        ensureRunningAuction(2001L, now.minusMinutes(5),
            HighestBid.of(Money.of(15_000L), 99L, 1L)); // 마감 지연 버퍼(1분) 넘게 지남, 입찰 있음 -> 낙찰 처리 + 이벤트 발행
        ensureRunningAuction(2002L, now.minusMinutes(5), null); // 마감 지연 버퍼 넘게 지남, 입찰 없음 -> 유찰 처리
        ensureRunningAuction(2003L, now.plusHours(1), null);    // 아직 마감 안 됨 -> 그대로 RUNNING

        log.info("[AuctionSeedLoader] seed uploaded");
    }

    private void ensureScheduledAuction(Long productId, LocalDateTime startAt) {
        if (auctionJpaRepository.existsByProductId(productId)) {
            return;
        }
        final AuctionSchedule schedule = AuctionSchedule.of(
            Period.of(startAt, startAt.plusHours(2)), false, null);
        final Auction auction = Auction.register(SEED_SELLER_ID, productId, itemInfo(), pricing(), schedule);
        auctionRepository.save(auction);
    }

    private void ensureRunningAuction(Long productId, LocalDateTime endAt, HighestBid highestBid) {
        if (auctionJpaRepository.existsByProductId(productId)) {
            return;
        }
        final AuctionSchedule schedule = AuctionSchedule.of(
            Period.of(endAt.minusHours(3), endAt), false, null);
        final Auction auction = Auction.of(SEED_SELLER_ID, productId, itemInfo(), pricing(), schedule,
            AuctionStatus.RUNNING, highestBid);
        auctionRepository.save(auction);
    }

    private ItemInfo itemInfo() {
        return ItemInfo.of(ItemCondition.MINT, "스케줄러 테스트용 시드 데이터입니다.", null);
    }

    private Pricing pricing() {
        return Pricing.of(Money.of(10_000L), Money.of(500L), Money.of(3_000L));
    }
}
