package site.coreservice.auction.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.coreservice.auction.application.port.AuctionSearchViewRepository;
import site.coreservice.auction.domain.Auction;
import site.coreservice.auction.domain.AuctionRepository;
import site.coreservice.auction.domain.AuctionStatus;
import site.coreservice.auction.exception.AuctionErrorCode;
import site.coreservice.auction.exception.AuctionException;


// 경매 시작/마감 건 단위 수행
@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionScheduleService {

    private final AuctionRepository auctionRepository;
    private final AuctionSearchViewRepository auctionSearchViewRepository;

    @Transactional
    protected Auction startAuction(final Long auctionId) {
        final Auction auction = auctionRepository.findById(auctionId)
            .orElseThrow(() -> new AuctionException(AuctionErrorCode.AUCTION_NOT_FOUND));

        auction.changeStatus(AuctionStatus.RUNNING);
        Auction saved = auctionRepository.save(auction);
        auctionSearchViewRepository.updateStatus(saved);
        log.info("경매 시작 처리: auctionId={}", saved.getId());
        return saved;
    }

    @Transactional
    protected Auction closeAuction(final Long auctionId) {
        final Auction auction = auctionRepository.findById(auctionId)
            .orElseThrow(() -> new AuctionException(AuctionErrorCode.AUCTION_NOT_FOUND));

        if (auction.hasBid()) {
            return closeWithWinner(auction);
        } else {
            return closeWithoutWinner(auction);
        }
    }

    private Auction closeWithWinner(final Auction auction) {
        auction.changeStatus(AuctionStatus.ENDED_WON);
        Auction saved = auctionRepository.save(auction);
        auctionSearchViewRepository.updateStatus(saved);
        log.info("경매 낙찰: auctionId={}, winnerId={}", saved.getId(),
            saved.getHighestBid().getBidderId());
        return saved;
    }

    private Auction closeWithoutWinner(final Auction auction) {
        auction.changeStatus(AuctionStatus.ENDED_FAILED);
        Auction saved = auctionRepository.save(auction);
        auctionSearchViewRepository.updateStatus(saved);
        log.info("경매 유찰: auctionId={}", saved.getId());
        return saved;
    }
}
