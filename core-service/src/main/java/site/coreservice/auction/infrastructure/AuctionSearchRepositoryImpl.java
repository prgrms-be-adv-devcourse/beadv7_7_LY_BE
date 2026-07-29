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
        AuctionSearchView view = searchViewJpaRepository.findById(auction.getId()).orElseThrow(
            () -> new AuctionException(AuctionErrorCode.AUCTION_SEARCH_VIEW_NOT_FOUND));
        view.updateFromAuction(auction, product);
    }

    @Override
    public void deleteById(Long auctionId) {
        searchViewJpaRepository.deleteById(auctionId);
    }

    @Override
    public List<AuctionProductSummary> findAllSummaryByIds(List<Long> auctionIds) {
        return searchViewJpaRepository.findAllById(auctionIds).stream()
            .map(v -> new AuctionProductSummary(v.getAuctionId(), v.getTitle(), v.getArtistName()))
            .toList();
    }

    @Override
    public Page<AuctionListSummary> search(AuctionListQuery query, Pageable pageable) {
        AuctionSortType sortType = AuctionSortType.from(query.sort());
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
            resolveSort(sortType));
        AuctionStatus status = query.status() == null ? null : AuctionStatus.from(query.status());
        LocalDateTime now = LocalDateTime.now();

        List<AuctionListSummary> content = findByStatus(query.genre(), query.pressType(), status,
            now, sortedPageable);
        long total = countByStatus(query.genre(), query.pressType(), status, now);

        return new PageImpl<>(content, sortedPageable, total);
    }

    // status 컬럼 대신 startAt/endAt/bidCount로 상태별 결과를 나눠 조회한다.
    // 상태 필터가 걸린 분기는 쿼리 자체가 이미 그 status만 골라온 것이므로 status를 그대로 넣어 매핑하고,
    // 필터가 없는 전체 조회만 행별로 시간/bidCount를 보고 status를 계산한다.
    // CANCELED는 애초에 삭제되는 행이라 조회 대상에서 항상 빈 결과를 반환한다.
    private List<AuctionListSummary> findByStatus(String genre, String pressType,
        AuctionStatus status,
        LocalDateTime now, Pageable pageable) {
        if (status == null) {
            return searchViewJpaRepository.searchAll(genre, pressType, pageable).stream()
                .map(v -> toSummary(v, resolveStatus(v, now)))
                .toList();
        }
        return switch (status) {
            case SCHEDULED ->
                searchViewJpaRepository.searchScheduled(genre, pressType, now, pageable).stream()
                    .map(v -> toSummary(v, AuctionStatus.SCHEDULED)).toList();
            case RUNNING ->
                searchViewJpaRepository.searchRunning(genre, pressType, now, pageable).stream()
                    .map(v -> toSummary(v, AuctionStatus.RUNNING)).toList();
            case ENDED_WON ->
                searchViewJpaRepository.searchEndedWon(genre, pressType, now, pageable).stream()
                    .map(v -> toSummary(v, AuctionStatus.ENDED_WON)).toList();
            case ENDED_FAILED ->
                searchViewJpaRepository.searchEndedFailed(genre, pressType, now, pageable).stream()
                    .map(v -> toSummary(v, AuctionStatus.ENDED_FAILED)).toList();
            case CANCELED -> List.of();
        };
    }

    private AuctionStatus resolveStatus(AuctionSearchView v, LocalDateTime now) {
        if (now.isBefore(v.getStartAt())) {
            return AuctionStatus.SCHEDULED;
        }
        if (now.isBefore(v.getEndAt())) {
            return AuctionStatus.RUNNING;
        }
        return v.getBidCount() > 0 ? AuctionStatus.ENDED_WON : AuctionStatus.ENDED_FAILED;
    }

    private long countByStatus(String genre, String pressType, AuctionStatus status,
        LocalDateTime now) {
        if (status == null) {
            return searchViewJpaRepository.countSearchAll(genre, pressType);
        }
        return switch (status) {
            case SCHEDULED -> searchViewJpaRepository.countSearchScheduled(genre, pressType, now);
            case RUNNING -> searchViewJpaRepository.countSearchRunning(genre, pressType, now);
            case ENDED_WON -> searchViewJpaRepository.countSearchEndedWon(genre, pressType, now);
            case ENDED_FAILED ->
                searchViewJpaRepository.countSearchEndedFailed(genre, pressType, now);
            case CANCELED -> 0L;
        };
    }

    private AuctionListSummary toSummary(AuctionSearchView v, AuctionStatus status) {
        return new AuctionListSummary(v.getAuctionId(), v.getProductId(), v.getTitle(),
            v.getArtistName(), v.getReleaseYear(), v.getGenre(), v.getPressType(), v.getThumbnail(),
            v.getSellerId(), v.getSellerNickname(), status, v.getHighestBidAmount(),
            v.getBidCount(), v.getStartAt(), v.getEndAt());
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
    public void updateOnBid(Long auctionId, BigDecimal highestBidAmount, int bidCount,
        LocalDateTime endAt) {
        AuctionSearchView view = searchViewJpaRepository.findById(auctionId).orElseThrow(
            () -> new AuctionException(AuctionErrorCode.AUCTION_SEARCH_VIEW_NOT_FOUND));
        view.updateOnBid(highestBidAmount, bidCount, endAt);
    }

}
