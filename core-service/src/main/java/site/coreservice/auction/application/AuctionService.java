package site.coreservice.auction.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.coreservice.auction.application.dto.*;
import site.coreservice.auction.application.port.AuctionSearchViewRepository;
import site.coreservice.auction.application.port.MemberPort;
import site.coreservice.auction.application.port.ProductPort;
import site.coreservice.auction.application.port.dto.ProductSnapshot;
import site.coreservice.auction.application.port.dto.AuctionProductSummary;
import site.coreservice.auction.domain.*;
import site.coreservice.auction.exception.AuctionErrorCode;
import site.coreservice.auction.exception.AuctionException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuctionService {
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final MemberPort memberPort;
    private final ProductPort productPort;
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

}
