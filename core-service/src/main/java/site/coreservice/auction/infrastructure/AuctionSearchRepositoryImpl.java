package site.coreservice.auction.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Component;
import site.coreservice.auction.application.dto.AuctionListQuery;
import site.coreservice.auction.application.dto.AuctionSortType;
import site.coreservice.auction.application.port.AuctionSearchViewRepository;
import site.coreservice.auction.application.port.dto.AuctionListSummary;
import site.coreservice.auction.application.port.dto.AuctionProductSummary;
import site.coreservice.auction.application.port.dto.ProductSnapshot;
import site.coreservice.auction.domain.Auction;
import site.coreservice.auction.domain.AuctionStatus;
import site.coreservice.auction.exception.AuctionErrorCode;
import site.coreservice.auction.exception.AuctionException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AuctionSearchRepositoryImpl implements AuctionSearchViewRepository {

    private final AuctionSearchViewJpaRepository searchViewJpaRepository;

    @Override
    public void save(Auction auction, ProductSnapshot product, String sellerNickname) {
        searchViewJpaRepository.save(AuctionSearchView.from(auction, product, sellerNickname));
    }

    @Override
    public void updateFromAuction(Auction auction, ProductSnapshot product) {
        AuctionSearchView view = searchViewJpaRepository.findById(auction.getId()).orElseThrow(() -> new AuctionException(AuctionErrorCode.AUCTION_SEARCH_VIEW_NOT_FOUND));
        view.updateFromAuction(auction, product);
    }

    @Override
    public void updateStatus(Auction auction) {
        AuctionSearchView view = searchViewJpaRepository.findById(auction.getId()).orElseThrow(() -> new AuctionException(AuctionErrorCode.AUCTION_SEARCH_VIEW_NOT_FOUND));
        view.updateStatus(auction.getStatus());
    }

    @Override
    public void deleteById(Long auctionId) {
        searchViewJpaRepository.deleteById(auctionId);
    }

    @Override
    public List<AuctionProductSummary> findAllSummaryByIds(List<Long> auctionIds) {
        return searchViewJpaRepository.findAllById(auctionIds).stream().map(v -> new AuctionProductSummary(v.getAuctionId(), v.getTitle(), v.getArtistName())).toList();
    }

    @Override
    public Page<AuctionListSummary> search(AuctionListQuery query, Pageable pageable) {
        AuctionSortType sortType = AuctionSortType.from(query.sort());
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), resolveSort(sortType));
        AuctionStatus status = query.status() == null ? null : AuctionStatus.from(query.status());

        List<AuctionListSummary> content = searchViewJpaRepository.search(query.genre(), query.pressType(), status, sortedPageable).stream().map(this::toSummary)   // infra 엔티티(AuctionSearchView) → application 타입 변환은 여기서 끝낸다
                .toList();
        long total = searchViewJpaRepository.countSearch(query.genre(), query.pressType(), status);

        return new PageImpl<>(content, sortedPageable, total);
    }

    private AuctionListSummary toSummary(AuctionSearchView v) {
        return new AuctionListSummary(v.getAuctionId(), v.getProductId(), v.getTitle(), v.getArtistName(), v.getReleaseYear(), v.getGenre(), v.getPressType(), v.getThumbnail(), v.getSellerId(), v.getSellerNickname(), v.getStatus(), v.getHighestBidAmount(), v.getBidCount(), v.getStartAt(), v.getEndAt());
    }

    private Sort resolveSort(AuctionSortType sortType) {
        return switch (sortType) {
            case PRICE_ASC -> Sort.by(Sort.Direction.ASC, "highestBidAmount");
            case PRICE_DESC -> Sort.by(Sort.Direction.DESC, "highestBidAmount");
            case MOST_BIDS -> Sort.by(Sort.Direction.DESC, "bidCount");
            case ENDING_SOON -> Sort.by(Sort.Direction.ASC, "endAt");   // 마감임박순 기본값
        };
    }

    @Override
    public void updateOnBid(Long auctionId, BigDecimal highestBidAmount, int bidCount, LocalDateTime endAt) {
        AuctionSearchView view = searchViewJpaRepository.findById(auctionId).orElseThrow(() -> new AuctionException(AuctionErrorCode.AUCTION_SEARCH_VIEW_NOT_FOUND));
        view.updateOnBid(highestBidAmount, bidCount, endAt);
    }

}
