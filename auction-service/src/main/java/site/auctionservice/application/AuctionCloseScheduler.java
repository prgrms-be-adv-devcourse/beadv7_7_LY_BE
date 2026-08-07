package site.auctionservice.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import site.auctionservice.domain.Auction;
import site.auctionservice.domain.AuctionRepository;

import java.time.LocalDateTime;
import java.util.List;

// scale out 고려 시 분산락 필요
@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionCloseScheduler {

    private static final long POLL_INTERVAL_MILLIS = 60_000L;

    // 마감 직전 입찰이 마감시간을 연장시킬 수 있어, endAt이 지나자마자 바로 닫으면
    // 연장 사항이 DB에 반영되기 전 상태를 읽고 마감시켜버릴 위험이 있어서,
    // 실제 마감시간보다 최소 1 주기만큼 늦게 처리
    private static final long CLOSE_DELAY_MINUTES = 1L;

    private final AuctionRepository auctionRepository;
    private final AuctionScheduleService auctionScheduleService;
    private final AuctionEventPublisher auctionEventPublisher;

    @Scheduled(fixedDelay = POLL_INTERVAL_MILLIS)
    public void closeEndedAuctions() {
        final List<Auction> auctions = auctionRepository.findAllRunningToEnd(
            LocalDateTime.now().minusMinutes(CLOSE_DELAY_MINUTES));

        for (final Auction auction : auctions) {
            try {
                final Auction closedAuction = auctionScheduleService.closeAuction(auction.getId());
                auctionEventPublisher.publishAuctionClosed(closedAuction);
            } catch (final Exception e) {
                log.error("경매 마감 처리 실패: auctionId={}", auction.getId(), e);
            }
        }
    }
}
