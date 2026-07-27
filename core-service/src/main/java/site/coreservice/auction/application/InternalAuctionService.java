package site.coreservice.auction.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.coreservice.auction.application.dto.InternalAuctionCountResult;
import site.coreservice.auction.application.dto.InternalAuctionSummaryResult;
import site.coreservice.auction.domain.Auction;
import site.coreservice.auction.domain.AuctionRepository;
import site.coreservice.auction.domain.BidRepository;
import site.coreservice.auction.exception.AuctionErrorCode;
import site.coreservice.auction.exception.AuctionException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// 다른 도메인이 호출하는 내부 API 전용 서비스. 여기 있는 메서드/DTO 계약은 다른 팀이 의존하므로
// AuctionService(공개 API) 리팩터링 과정에서 실수로 영향을 주지 않도록 분리한다.
@Service
@RequiredArgsConstructor
public class InternalAuctionService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;

    @Transactional(readOnly = true)
    public InternalAuctionSummaryResult getInternalSummary(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .filter(a -> !a.isCanceled())
                .orElseThrow(() -> new AuctionException(AuctionErrorCode.AUCTION_NOT_FOUND));
        long bidCount = bidRepository.countByAuctionId(auctionId);
        BigDecimal finalPrice = auction.hasBid() ? auction.getHighestBid().getAmount().getValue() : null;

        return new InternalAuctionSummaryResult(
                auction.getId(), auction.getProductId(),
                auction.getItemInfo().getCondition().name(),
                bidCount, finalPrice,
                auction.getSchedule().getPeriod().getEndAt(),
                auction.getEffectiveStatusAt(LocalDateTime.now()).name()
        );
    }

    @Transactional(readOnly = true)
    public List<InternalAuctionCountResult> getOpenAuctionCounts(List<Long> productIds) {
        List<Long> distinctProductIds = productIds.stream().distinct().toList();
        Map<Long, Long> countsByProductId = auctionRepository.countRunningByProductIds(distinctProductIds);

        return distinctProductIds.stream()
                .map(productId -> new InternalAuctionCountResult(productId, countsByProductId.getOrDefault(productId, 0L)))
                .toList();
    }
}
