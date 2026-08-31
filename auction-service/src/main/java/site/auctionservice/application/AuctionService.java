package site.auctionservice.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import site.auctionservice.aop.DistributedLock;
import site.auctionservice.aop.RateLimit;
import site.auctionservice.domain.*;
import site.auctionservice.application.dto.*;
import site.auctionservice.application.port.AuctionSearchViewRepository;
import site.auctionservice.application.port.BidOutbidMarkPort;
import site.auctionservice.application.port.BidReactionPort;
import site.auctionservice.application.port.LockPort;
import site.auctionservice.application.port.MemberPort;
import site.auctionservice.application.port.ProductPort;
import site.auctionservice.application.port.WalletPort;
import site.auctionservice.application.port.dto.AuctionListSummary;
import site.auctionservice.application.port.dto.AuctionProductSummary;
import site.auctionservice.application.port.dto.ProductDetail;
import site.auctionservice.application.port.dto.ProductSnapshot;
import site.auctionservice.application.port.dto.WalletHoldInfo;
import site.auctionservice.exception.AuctionErrorCode;
import site.auctionservice.exception.AuctionException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionService {

    private static final int RECENT_BID_LIMIT = 5;
    private static final String UNKNOWN_NICKNAME = "알 수 없음";

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final MemberPort memberPort;
    private final ProductPort productPort;
    private final WalletPort walletPort;
    private final AuctionSearchViewRepository searchViewRepository;
    private final AuctionEventPublisher auctionEventPublisher;
    private final ImageUrlValidator imageUrlValidator;
    private final TransactionTemplate transactionTemplate;
    private final LockPort lockPort;
    private final BidOutbidMarkPort bidOutbidMarker;
    private final BidReactionPort bidReactionTracker;

    private static final long BID_LOCK_WAIT_TIME = 3L;
    private static final long BID_LOCK_LEASE_TIME = -1L;

    @Transactional
    public AuctionResult createAuction(CreateAuctionCommand command, Long sellerId) {
        String sellerNickname = memberPort.getNickname(sellerId);
        ProductSnapshot productSnapshot = productPort.getProduct(command.productId());
        if (!productSnapshot.active()) {
            throw new AuctionException(AuctionErrorCode.PRODUCT_NOT_ACTIVE);
        }
        imageUrlValidator.validate(command.itemImages());
        Auction auction = Auction.register(
            sellerId, command.productId(),
            ItemInfo.of(ItemCondition.from(command.itemCondition()), command.itemDescription(),
                command.itemImages()),
            Pricing.of(Money.from(command.startPrice()), Money.from(command.bidUnit()),
                Money.from(command.shippingFee())),
            AuctionSchedule.of(Period.of(command.startAt(), command.endAt()),
                command.extensionEnabled(), command.extensionTime()),
            LocalDateTime.now()
        );
        auctionRepository.save(auction);
        searchViewRepository.save(auction, productSnapshot, sellerNickname);

        return AuctionResult.from(auction);
    }

    @DistributedLock(key = "#command.auctionId()")
    @Transactional
    public AuctionResult modifyAuction(ModifyAuctionCommand command, Long sellerId) {
        Auction auction = auctionRepository.findById(command.auctionId())
            .orElseThrow(() -> new AuctionException(AuctionErrorCode.AUCTION_NOT_FOUND));
        boolean productChanged = !auction.getProductId().equals(command.productId());

        imageUrlValidator.validate(command.itemImages());
        auction.modify(sellerId, command.productId(),
            ItemInfo.of(ItemCondition.from(command.itemCondition()), command.itemDescription(),
                command.itemImages()),
            Pricing.of(Money.from(command.startPrice()), Money.from(command.bidUnit()),
                Money.from(command.shippingFee())),
            AuctionSchedule.of(Period.of(command.startAt(), command.endAt()),
                command.extensionEnabled(), command.extensionTime()),
            LocalDateTime.now()
        );

        ProductSnapshot product = null;
        if (productChanged) {
            product = productPort.getProduct(command.productId());
            if (!product.active()) {
                throw new AuctionException(AuctionErrorCode.PRODUCT_NOT_ACTIVE);
            }
        }
        searchViewRepository.updateFromAuction(auction, product);
        return AuctionResult.from(auction);
    }

    @DistributedLock(key = "#auctionId")
    @Transactional
    public void deleteAuction(Long auctionId, Long sellerId) {
        Auction auction = auctionRepository.findById(auctionId)
            .orElseThrow(() -> new AuctionException(AuctionErrorCode.AUCTION_NOT_FOUND));
        auction.cancel(sellerId, LocalDateTime.now());
        searchViewRepository.deleteById(auctionId);
    }

    @Transactional(readOnly = true)
    public AuctionDetailResult getAuctionDetail(Long auctionId, Long viewerId) {
        Auction auction = auctionRepository.findById(auctionId)
            .filter(a -> !a.isCancelledOrForceCancelled())
            .orElseThrow(() -> new AuctionException(AuctionErrorCode.AUCTION_NOT_FOUND));

        ProductDetail product = productPort.getProductDetail(auction.getProductId());
        String sellerNickname = getNicknameOrFallback(auction.getSellerId());
        LocalDateTime now = LocalDateTime.now();

        AuctionStatusDetail auctionStatusDetail = auction.isEffectiveClosingAt(now)
            ? new AuctionStatusDetail.ClosingDetail()
            : switch (auction.getEffectiveStatusAt(now)) {
                case SCHEDULED -> new AuctionStatusDetail.ScheduledDetail();
                case RUNNING -> getRunningDetail(auction, viewerId);
                case ENDED_WON -> getEndedWonDetail(auction);
                case ENDED_FAILED -> new AuctionStatusDetail.EndedFailedDetail();
                case CANCELED, FORCE_CANCELED -> throw new AuctionException(AuctionErrorCode.AUCTION_NOT_FOUND);
            };

        return AuctionDetailResult.of(auction, product, sellerNickname, auctionStatusDetail);
    }

    private AuctionStatusDetail.RunningDetail getRunningDetail(Auction auction, Long viewerId) {
        List<Bid> recentBids = bidRepository.findRecentByAuctionId(auction.getId(),
            RECENT_BID_LIMIT);
        HighestBid highestBid = auction.getHighestBid();
        List<BidDetailResult> recentBidDetails = toBidDetailResults(recentBids);
        return new AuctionStatusDetail.RunningDetail(
            highestBid == null ? null : highestBid.getAmount().getValue(),
            auction.getPricing().nextMinBidAmount(highestBid).getValue(),
            bidRepository.countByAuctionId(auction.getId()), recentBidDetails,
            auction.isHighestBidder(viewerId));
    }

    // 닉네임은 경매 상세의 부가 표시 정보라, 조회 실패가 경매 상세 조회 전체를 막지 않도록 fallback 처리한다.
    private String getNicknameOrFallback(Long memberId) {
        try {
            return memberPort.getNickname(memberId);
        } catch (RuntimeException e) {
            log.warn("닉네임 조회 실패 - fallback 처리: memberId={}", memberId, e);
            return UNKNOWN_NICKNAME;
        }
    }

    private List<BidDetailResult> toBidDetailResults(List<Bid> bids) {
        // 같은 입찰자가 여러 번 등장할 수 있어(outbid 후 재입찰), 중복 없이 한 번씩만 조회한다.
        Map<Long, String> nicknamesByBidderId = bids.stream()
            .map(Bid::getBidderId)
            .distinct()
            .collect(Collectors.toMap(bidderId -> bidderId, this::getNicknameOrFallback));
        return bids.stream()
            .map(bid -> BidDetailResult.of(bid, nicknamesByBidderId.get(bid.getBidderId())))
            .toList();
    }

    private AuctionStatusDetail.EndedWonDetail getEndedWonDetail(Auction auction) {
        Long bidId = auction.getHighestBid().getBidId();
        Bid winningBid = bidRepository.findById(bidId)
            .orElseThrow(() -> new AuctionException(AuctionErrorCode.BID_NOT_FOUND));
        List<Bid> recentBids = bidRepository.findRecentByAuctionId(auction.getId(),
            RECENT_BID_LIMIT);
        return new AuctionStatusDetail.EndedWonDetail(
            BidDetailResult.of(winningBid, getNicknameOrFallback(winningBid.getBidderId())),
            toBidDetailResults(recentBids));
    }


    @Transactional(readOnly = true)
    public PageResult<ParticipatedAuctionResult> getParticipatedAuctions(Long bidderId,
                                                                         Pageable pageable) {
        Page<Bid> latestBids = bidRepository.findLatestBidsByBidder(bidderId, pageable);
        LocalDateTime now = LocalDateTime.now();

        List<Long> auctionIds = latestBids.getContent().stream().map(Bid::getAuctionId).toList();
        Map<Long, Auction> auctionsById = auctionRepository.findAllByIds(auctionIds).stream()
            .collect(Collectors.toMap(Auction::getId, a -> a));
        Map<Long, AuctionProductSummary> summaryById = summariesByAuctionId(auctionIds);

        // 취소된 경매는 조회 결과에서 제외한다
        List<ParticipatedAuctionResult> items = latestBids.getContent().stream()
            .filter(bid -> {
                Auction auction = auctionsById.get(bid.getAuctionId());
                return auction != null && !auction.isCancelledOrForceCancelled();
            })
            .map(bid -> {
                Auction auction = auctionsById.get(bid.getAuctionId());
                AuctionProductSummary summary = summaryById.get(bid.getAuctionId());
                return ParticipatedAuctionResult.of(auction, bid, summary,
                    auction.getEffectiveStatusAt(now));
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
        Map<Long, AuctionProductSummary> summaryById = summariesByAuctionId(auctionIds);

        // 취소된 경매는 조회 결과에서 제외한다
        List<HostedAuctionResult> items = auctions.getContent().stream()
            .filter(auction -> !auction.isCancelledOrForceCancelled())
            .map(auction -> {
                AuctionProductSummary summary = summaryById.get(auction.getId());
                Money highest = auction.hasBid() ? auction.getHighestBid().getAmount() : null;
                long bidCount = bidCounts.getOrDefault(auction.getId(), 0L);
                return HostedAuctionResult.of(auction, summary, highest, bidCount,
                    auction.getEffectiveStatusAt(now));
            })
            .toList();

        return PageResult.of(auctions, items);
    }

    // SearchView는 상품 표시정보(title/artistName)만 가져오는 용도 — ProductDisplaySummary(application 타입, 참여이력 PR에서 정의)로 받는다
    private Map<Long, AuctionProductSummary> summariesByAuctionId(List<Long> auctionIds) {
        return searchViewRepository.findAllSummaryByIds(auctionIds).stream()
            .collect(Collectors.toMap(AuctionProductSummary::auctionId, s -> s));
    }

    @Transactional(readOnly = true)
    public PageResult<AuctionListItemResult> getAuctions(AuctionListQuery query,
                                                         Pageable pageable) {
        Page<AuctionListSummary> result = searchViewRepository.search(query, pageable);
        List<AuctionListItemResult> items = result.getContent().stream()
            .map(AuctionListItemResult::from).toList();
        return PageResult.of(result, items);
    }

    /**
     * @DistributedLock 애노테이션 대신 LockPort를 직접 호출해 executeBid()만 락+트랜잭션으로 좁게 감싼다
     * 보상 호출(rollback)이 경매 락이 풀린 뒤에 실행되게 하기 위해서다(pointwallet rollback()은 holdId 기준 멱등이라 안전).
     */
    @RateLimit(limit = 3, windowMs = 2000, keyPrefix = "bid",
            resourceIdKey = "#command.auctionId()", userIdKey = "#command.bidderId()")
    public PlaceBidResult placeBid(PlaceBidCommand command) {
        bidReactionTracker.recordReactionIfApplicable(command.auctionId(), command.bidderId());

        if (memberPort.isMemberRestricted(command.bidderId())) {
            throw new AuctionException(AuctionErrorCode.BID_MEMBER_RESTRICTED);
        }

        AtomicReference<WalletHoldInfo> holdInfoRef = new AtomicReference<>();
        AtomicReference<Long> outbidBidderIdRef = new AtomicReference<>();
        try {
            PlaceBidResult result = lockPort.executeWithLockOnAuction(command.auctionId(), BID_LOCK_WAIT_TIME,
                    BID_LOCK_LEASE_TIME, TimeUnit.SECONDS,
                    () -> transactionTemplate.execute(status -> executeBid(command, holdInfoRef, outbidBidderIdRef)));

            // 여기 도달했다는 것 자체가 트랜잭션 커밋 성공 + 락 해제를 이미 뜻한다.
            Long outbidBidderId = outbidBidderIdRef.get();
            if (outbidBidderId != null) {
                markOutbidBestEffort(command.auctionId(), outbidBidderId);
            }
            return result;
        } catch (RuntimeException e) {
            compensateHold(command, holdInfoRef.get(), e);
            throw e;
        }
    }

    // BidOutbidMarker 자체도 fail-open이지만, 이미 성공한 입찰이 이 예외 때문에 placeBid()의
    // catch로 빠져 예치금 롤백까지 되는 걸 막기 위해 호출부에서도 한 번 더 막는다.
    private void markOutbidBestEffort(Long auctionId, Long outbidBidderId) {
        try {
            bidOutbidMarker.markOutbid(auctionId, outbidBidderId);
        } catch (RuntimeException markFailure) {
            log.warn("outbid 마킹 실패 - 입찰 자체는 이미 성공했으므로 영향 없음: auctionId={}, bidderId={}",
                    auctionId, outbidBidderId, markFailure);
        }
    }

    private void compensateHold(PlaceBidCommand command, WalletHoldInfo holdInfo, RuntimeException cause) {
        if (holdInfo == null) {
            // hold() 자체가 실패해서 holdId가 없는 경우 - 보상할 대상이 없으므로 그대로 전파
            return;
        }
        log.error("입찰 처리 실패 - 예치금 홀드 보상(rollback) 시도: holdId={}, auctionId={}, bidderId={}, amount={}",
                holdInfo.holdId(), command.auctionId(), command.bidderId(), command.amount(), cause);
        try {
            walletPort.rollback(holdInfo.holdId(), command.auctionId(), command.bidderId(), Money.from(command.amount()));
        } catch (RuntimeException rollbackFailure) {
            // 보상 호출 자체가 실패해도 원래 입찰 실패 사유를 덮어쓰면 안 되므로 별도로 잡아서 로그만 남긴다
            log.error("예치금 홀드 보상(rollback) 실패 - 홀드가 해제되지 않은 채 남아있을 수 있음: holdId={}, auctionId={}, bidderId={}",
                    holdInfo.holdId(), command.auctionId(), command.bidderId(), rollbackFailure);
        }
    }

    private PlaceBidResult executeBid(PlaceBidCommand command, AtomicReference<WalletHoldInfo> holdInfoRef, AtomicReference<Long> outbidBidderIdRef) {
        LocalDateTime now = LocalDateTime.now();

        Auction auction = auctionRepository.findById(command.auctionId()).orElseThrow(() -> new AuctionException(AuctionErrorCode.AUCTION_NOT_FOUND));
        Money amount = Money.from(command.amount());

        // 예치금 호출 전 사전 검증
        auction.validateBiddable(command.bidderId(), amount, now);

        // holdId는 실패 시 보상(rollback)에 필요해서 캡처해둔다
        WalletHoldInfo holdInfo = walletPort.hold(command.auctionId(), command.bidderId(), amount);
        holdInfoRef.set(holdInfo);

        // 기존 ACTIVE Bid → OUTBID (Bid는 IDENTITY 채번이라 save() 시 즉시 flush되므로,
        // 새 Bid 저장 전에 처리해야 findActiveBid가 항상 최대 1건만 조회함)
        Optional<Bid> previousActiveBid = bidRepository.findActiveBid(command.auctionId());
        previousActiveBid.ifPresent(Bid::markOutbid);
        // outbid Redis 마킹은 여기서 바로 하지 않고 이전 최고입찰자만 캡처해둔다
        // placeBid()가 락 해제 + 트랜잭션 커밋 성공을 확인한 뒤에 마킹해야 롤백된 입찰에 대한 가짜 마킹이 안 남고 락 보유 시간도 늘리지 않는다.
        previousActiveBid.ifPresent(bid -> outbidBidderIdRef.set(bid.getBidderId()));
        // 새 Bid 저장 (ACTIVE)
        Bid newBid = bidRepository.save(Bid.place(command.auctionId(), command.bidderId(), amount, now));
        // 최고입찰 갱신 + 마감 연장
        LocalDateTime endAtBefore = auction.getEndAt();
        auction.applyBid(command.bidderId(), amount, newBid.getId(), now);
        LocalDateTime endAtAfter = auction.getEndAt();
        boolean extended = !endAtBefore.equals(endAtAfter);

        // SearchView 갱신
        int bidCount = (int) bidRepository.countByAuctionId(command.auctionId());
        searchViewRepository.updateOnBid(command.auctionId(), amount.getValue(), bidCount, endAtAfter);

        return PlaceBidResult.of(newBid, auction, amount, endAtAfter, extended);
    }

    @DistributedLock(key = "#auctionId")
    @Transactional
    public void forceCancelAuction(Long auctionId) {
        LocalDateTime now = LocalDateTime.now();

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionException(AuctionErrorCode.AUCTION_NOT_FOUND));

        boolean hasBid = auction.hasBid();
        auction.forceCancel(now);   // 검증 + 상태 전이

        searchViewRepository.deleteById(auctionId);

        if (hasBid) {
            bidRepository.findActiveBid(auctionId).ifPresent(Bid::markCanceled);
            Long bidderId = auction.getHighestBid().getBidderId();
            try {
                auctionEventPublisher.publishForceCanceled(auctionId, bidderId);
            } catch(final Exception e) {
                log.error("경매 강제 종료 예치금 홀드 해제 실패 : auctionId={}, bidderId={}", auctionId, bidderId, e);
            }
        }
    }

}
