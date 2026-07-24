package site.coreservice.auction.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.coreservice.auction.domain.Auction;
import site.coreservice.auction.domain.AuctionPolicy;
import site.coreservice.auction.domain.AuctionRepository;
import site.coreservice.auction.domain.AuctionStatus;

import java.time.LocalDateTime;
import java.util.List;

// scale out 고려 시 분산락 필요
@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionStartScheduler {

    //1분마다
    private static final long POLL_INTERVAL_MILLIS = 60_000L;

    // 경매 시작 시간 설정 단위의 절반인 5분 전부터 상태 변경 시도
    // ex) 18:00 시작 경매 -> 17:55부터 1분 주기로 상태 변경 시도
    private static final long EARLY_START_MINUTES = AuctionPolicy.TIME_UNIT_MINUTES / 2L;

    private final AuctionRepository auctionRepository;

    @Scheduled(fixedDelay = POLL_INTERVAL_MILLIS)
    @Transactional
    public void startScheduledAuctions() {
        final List<Auction> auctions = auctionRepository.findAllScheduledToStart(
            LocalDateTime.now().plusMinutes(EARLY_START_MINUTES));

        for (final Auction auction : auctions) {
            auction.changeStatus(AuctionStatus.RUNNING);
            auctionRepository.save(auction);
            log.info("경매 시작 처리: auctionId={}", auction.getId());
        }
    }
}
