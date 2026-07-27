package site.coreservice.auction.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.coreservice.auction.application.dto.*;
import site.coreservice.auction.application.port.AuctionSearchViewRepository;
import site.coreservice.auction.application.port.MemberPort;
import site.coreservice.auction.application.port.ProductPort;
import site.coreservice.auction.application.port.WalletPort;
import site.coreservice.auction.application.port.dto.ProductDetail;
import site.coreservice.auction.application.port.dto.AuctionListSummary;
import site.coreservice.auction.application.port.dto.ProductSnapshot;
import site.coreservice.auction.application.port.dto.AuctionProductSummary;
import site.coreservice.auction.domain.*;
import site.coreservice.auction.exception.AuctionErrorCode;
import site.coreservice.auction.exception.AuctionException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionService {
    private static final int RECENT_BID_LIMIT = 5;

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final MemberPort memberPort;
    private final ProductPort productPort;
    private final WalletPort walletPort;
    private final AuctionSearchViewRepository searchViewRepository;

    @Transactional
    public AuctionResult createAuction(CreateAuctionCommand command, Long sellerId) {
        String sellerNickname = memberPort.getNickname(sellerId);
        ProductSnapshot productSnapshot = productPort.getProduct(command.productId());
        Auction auction = Auction.register(
                sellerId, command.productId(),
                ItemInfo.of(ItemCondition.from(command.itemCondition()), command.itemDescription(), command.itemImages()),
                Pricing.of(Money.from(command.startPrice()), Money.from(command.bidUnit()), Money.from(command.shippingFee())),
                AuctionSchedule.of(Period.of(command.startAt(), command.endAt()), command.extensionEnabled(), command.extensionTime())
        );
        auctionRepository.save(auction);
        searchViewRepository.save(auction, productSnapshot, sellerNickname);

        return AuctionResult.from(auction);
    }

    @Transactional
    public AuctionResult modifyAuction(ModifyAuctionCommand command, Long sellerId) {
        Auction auction = auctionRepository.findById(command.auctionId()).orElseThrow(() -> new AuctionException(AuctionErrorCode.AUCTION_NOT_FOUND));
        boolean productChanged = !auction.getProductId().equals(command.productId());

        auction.modify(sellerId, command.productId(),
                ItemInfo.of(ItemCondition.from(command.itemCondition()), command.itemDescription(), command.itemImages()),
                Pricing.of(Money.from(command.startPrice()), Money.from(command.bidUnit()), Money.from(command.shippingFee())),
                AuctionSchedule.of(Period.of(command.startAt(), command.endAt()), command.extensionEnabled(), command.extensionTime()),
                LocalDateTime.now()
        );

        ProductSnapshot product = productChanged ? productPort.getProduct(command.productId()) : null;
        searchViewRepository.updateFromAuction(auction, product);
        return AuctionResult.from(auction);
    }

    @Transactional
    public void deleteAuction(Long auctionId, Long sellerId) {
        Auction auction = auctionRepository.findById(auctionId).orElseThrow(() -> new AuctionException(AuctionErrorCode.AUCTION_NOT_FOUND));
        auction.cancel(sellerId, LocalDateTime.now());
        searchViewRepository.deleteById(auctionId);
    }

    @Transactional(readOnly = true)
    public AuctionDetailResult getAuctionDetail(Long auctionId, Long viewerId) {
        Auction auction = auctionRepository.findById(auctionId).filter(a -> !a.isCanceled()).orElseThrow(() -> new AuctionException(AuctionErrorCode.AUCTION_NOT_FOUND));

        ProductDetail product = productPort.getProductDetail(auction.getProductId());
        String sellerNickname = memberPort.getNickname(auction.getSellerId());
        LocalDateTime now = LocalDateTime.now();

        AuctionStatusDetail auctionStatusDetail = auction.isEffectiveClosingAt(now)
                ? new AuctionStatusDetail.ClosingDetail()
                : switch (auction.getEffectiveStatusAt(now)) {
            case SCHEDULED -> new AuctionStatusDetail.ScheduledDetail();
            case RUNNING -> getRunningDetail(auction, viewerId);
            case ENDED_WON -> getEndedWonDetail(auction.getHighestBid().getBidId());
            case ENDED_FAILED -> new AuctionStatusDetail.EndedFailedDetail();
            case CANCELED -> throw new AuctionException(AuctionErrorCode.AUCTION_NOT_FOUND);
        };

        return AuctionDetailResult.of(auction, product, sellerNickname, auctionStatusDetail);
    }

    private AuctionStatusDetail.RunningDetail getRunningDetail(Auction auction, Long viewerId) {
        List<Bid> recentBids = bidRepository.findRecentByAuctionId(auction.getId(), RECENT_BID_LIMIT);
        HighestBid highestBid = auction.getHighestBid();
        return new AuctionStatusDetail.RunningDetail(highestBid == null ? null : highestBid.getAmount().getValue(), auction.getPricing().nextMinBidAmount(highestBid).getValue(), bidRepository.countByAuctionId(auction.getId()), recentBids.stream().map(bid -> BidDetailResult.of(bid, memberPort.getNickname(bid.getBidderId()))).toList(), auction.isHighestBidder(viewerId));
    }

    private AuctionStatusDetail.EndedWonDetail getEndedWonDetail(Long bidId) {
        Bid bid = bidRepository.findById(bidId).orElseThrow(() -> new AuctionException(AuctionErrorCode.BID_NOT_FOUND));
        return new AuctionStatusDetail.EndedWonDetail(BidDetailResult.of(bid, memberPort.getNickname(bid.getBidderId())));
    }


    @Transactional(readOnly = true)
    public PageResult<ParticipatedAuctionResult> getParticipatedAuctions(Long bidderId, Pageable pageable) {
        Page<Bid> latestBids = bidRepository.findLatestBidsByBidder(bidderId, pageable);
        LocalDateTime now = LocalDateTime.now();

        List<Long> auctionIds = latestBids.getContent().stream().map(Bid::getAuctionId).toList();
        Map<Long, Auction> auctionsById = auctionRepository.findAllByIds(auctionIds).stream().collect(Collectors.toMap(Auction::getId, a -> a));
        Map<Long, AuctionProductSummary> summaryById = searchViewRepository.findAllSummaryByIds(auctionIds).stream()
                .collect(Collectors.toMap(AuctionProductSummary::auctionId, d -> d));

        // 취소된 경매는 조회 결과에서 제외한다
        List<ParticipatedAuctionResult> items = latestBids.getContent().stream()
                .filter(bid -> {
                    Auction auction = auctionsById.get(bid.getAuctionId());
                    return auction != null && !auction.isCanceled();
                })
                .map(bid -> {
                    Auction auction = auctionsById.get(bid.getAuctionId());
                    AuctionProductSummary summary = summaryById.get(bid.getAuctionId());
                    return ParticipatedAuctionResult.of(auction, bid, summary, auction.getEffectiveStatusAt(now));
                })
                .toList();

        return PageResult.of(latestBids, items);
    }

    @Transactional(readOnly = true)
    public PageResult<HostedAuctionResult> getHostedAuctions(Long sellerId, Pageable pageable) {
        Page<Auction> auctions = auctionRepository.findBySellerId(sellerId, pageable);
        LocalDateTime now = LocalDateTime.now();

        List<Long> auctionIds = auctions.getContent().stream().map(Auction::getId).toList();
        // highestBidAmount/bidCount는 Auction/Bid 원본에서 — SearchView 동기화 리스크를 안 탄다
        Map<Long, Long> bidCounts = bidRepository.countGroupedByAuctionIds(auctionIds);
        // SearchView는 상품 표시정보(title/artistName)만 가져오는 용도 — ProductDisplaySummary(application 타입, 참여이력 PR에서 정의)로 받는다
        Map<Long, AuctionProductSummary> summaryById = searchViewRepository.findAllSummaryByIds(auctionIds).stream()
                .collect(Collectors.toMap(AuctionProductSummary::auctionId, d -> d));

        // 취소된 경매는 조회 결과에서 제외한다
        List<HostedAuctionResult> items = auctions.getContent().stream()
                .filter(auction -> !auction.isCanceled())
                .map(auction -> {
                    AuctionProductSummary summary = summaryById.get(auction.getId());
                    Money highest = auction.hasBid() ? auction.getHighestBid().getAmount() : null;
                    long bidCount = bidCounts.getOrDefault(auction.getId(), 0L);
                    return HostedAuctionResult.of(auction, summary, highest, bidCount, auction.getEffectiveStatusAt(now));
                })
                .toList();

        return PageResult.of(auctions, items);
    }

    @Transactional(readOnly = true)
    public PageResult<AuctionListItemResult> getAuctions(AuctionListQuery query, Pageable pageable) {
        Page<AuctionListSummary> result = searchViewRepository.search(query, pageable);
        List<AuctionListItemResult> items = result.getContent().stream().map(AuctionListItemResult::from).toList();
        return PageResult.of(result, items);
    }

    @Transactional
    public PlaceBidResult placeBid(PlaceBidCommand command) {
        LocalDateTime now = LocalDateTime.now();

        Auction auction = auctionRepository.findById(command.auctionId()).orElseThrow(() -> new AuctionException(AuctionErrorCode.AUCTION_NOT_FOUND));
        Money amount = Money.from(command.amount());

        // 예치금 호출 전 사전 검증
        auction.validateBiddable(command.bidderId(), amount, now);

        // 예치금 홀드 (동기, 트랜잭션 안)
        walletPort.hold(command.auctionId(), command.bidderId(), amount);

        // TODO(#75) 입찰 실패 시 예치금 홀드 해제(보상 트랜잭션)는 아직 없음.
        // 아래 구간에서 예외가 나면 홀드는 이미 잡힌 채로 남는다 — 지금은 로그로만 흔적을 남긴다.
        try {
            // 새 Bid 저장 (ACTIVE)
            Bid newBid = bidRepository.save(Bid.place(command.auctionId(), command.bidderId(), amount, now));
            // ACTIVE Bid → OUTBID
            bidRepository.findActiveBid(command.auctionId())
                    .filter(prev -> !prev.getId().equals(newBid.getId()))
                    .ifPresent(Bid::markOutbid);
            // 최고입찰 갱신 + 마감 연장
            LocalDateTime endAtBefore = auction.getSchedule().getPeriod().getEndAt();
            auction.applyBid(command.bidderId(), amount, newBid.getId(), now);
            LocalDateTime endAtAfter = auction.getSchedule().getPeriod().getEndAt();
            boolean extended = !endAtBefore.equals(endAtAfter);

            // SearchView 갱신
            int bidCount = (int) bidRepository.countByAuctionId(command.auctionId());
            searchViewRepository.updateOnBid(command.auctionId(), amount.getValue(), bidCount, endAtAfter);

            return PlaceBidResult.of(newBid, auction, amount, endAtAfter, extended);
        } catch (RuntimeException e) {
            log.error("입찰 처리 실패 - 예치금 홀드가 해제되지 않은 채 남아있을 수 있음: auctionId={}, bidderId={}, amount={}",
                    command.auctionId(), command.bidderId(), amount.getValue(), e);
            throw e;
        }
    }

}
