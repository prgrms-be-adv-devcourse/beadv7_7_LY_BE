package site.coreservice.auction.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.coreservice.auction.domain.Auction;
import site.coreservice.auction.domain.AuctionRepository;
import site.coreservice.auction.domain.AuctionStatus;


// 경매 마감 건 단위로 트랜잭션 관리
@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionCloseService {

    private final AuctionRepository auctionRepository;

    @Transactional
    public Auction closeAuction(final Long auctionId) {
        final Auction auction = auctionRepository.findById(auctionId)
            .orElseThrow(() -> new IllegalStateException("경매를 찾을 수 없습니다: " + auctionId));

        if (auction.hasBid()) {
            closeWithWinner(auction);
        } else {
            closeWithoutWinner(auction);
        }
        return auction;
    }

    private void closeWithWinner(final Auction auction) {
        auction.changeStatus(AuctionStatus.ENDED_WON);
        auctionRepository.save(auction);
        log.info("경매 낙찰: auctionId={}, winnerId={}", auction.getId(),
            auction.getHighestBid().getBidderId());
    }

    private void closeWithoutWinner(final Auction auction) {
        auction.changeStatus(AuctionStatus.ENDED_FAILED);
        auctionRepository.save(auction);
        log.info("경매 유찰: auctionId={}", auction.getId());
    }
}
