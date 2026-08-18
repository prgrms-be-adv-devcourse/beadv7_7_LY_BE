package site.auctionservice.infrastructure;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import site.auctionservice.application.dto.AuctionListQuery;
import site.auctionservice.application.dto.AuctionSortType;
import site.auctionservice.application.port.AuctionSearchViewRepository;
import site.auctionservice.application.port.dto.AuctionListSummary;
import site.auctionservice.application.port.dto.AuctionProductSummary;
import site.auctionservice.application.port.dto.ProductSnapshot;
import site.auctionservice.domain.Auction;
import site.auctionservice.domain.AuctionStatus;
import site.auctionservice.exception.AuctionErrorCode;
import site.auctionservice.exception.AuctionException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

        // CANCELED 경매는 서치 뷰에서 즉시 삭제되므로 조회 대상에 존재하지 않는다.
        if (status == AuctionStatus.CANCELED) {
            return new PageImpl<>(List.of(), sortedPageable, 0);
        }

        LocalDateTime now = LocalDateTime.now();
        Specification<AuctionSearchView> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            addIfPresent(predicates, productIdEq(query.productId(), root, cb));
            addIfPresent(predicates, genreEq(query.genre(), root, cb));
            addIfPresent(predicates, pressTypeEq(query.pressType(), root, cb));
            addIfPresent(predicates, statusEq(status, now, root, cb));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<AuctionSearchView> rows = searchViewJpaRepository.findAll(spec, sortedPageable);

        return rows.map(v -> toSummary(v, status != null ? status : resolveStatus(v, now)));
    }

    private void addIfPresent(List<Predicate> predicates, Predicate predicate) {
        if (predicate != null) {
            predicates.add(predicate);
        }
    }

    private Predicate productIdEq(Long productId, Root<AuctionSearchView> root, CriteriaBuilder cb) {
        return productId == null ? null : cb.equal(root.get("productId"), productId);
    }

    private Predicate genreEq(String genre, Root<AuctionSearchView> root, CriteriaBuilder cb) {
        return genre == null ? null : cb.equal(root.get("genre"), genre);
    }

    private Predicate pressTypeEq(String pressType, Root<AuctionSearchView> root, CriteriaBuilder cb) {
        return pressType == null ? null : cb.equal(root.get("pressType"), pressType);
    }

    // status 컬럼이 없어 startAt/endAt/bidCount 조합으로 상태를 판별한다.
    private Predicate statusEq(AuctionStatus status, LocalDateTime now, Root<AuctionSearchView> root,
                               CriteriaBuilder cb) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case SCHEDULED -> cb.greaterThan(root.get("startAt"), now);
            case RUNNING -> cb.and(
                    cb.lessThanOrEqualTo(root.get("startAt"), now),
                    cb.greaterThan(root.get("endAt"), now));
            case ENDED_WON -> cb.and(
                    cb.lessThanOrEqualTo(root.get("endAt"), now),
                    cb.greaterThan(root.get("bidCount"), 0));
            case ENDED_FAILED -> cb.and(
                    cb.lessThanOrEqualTo(root.get("endAt"), now),
                    cb.equal(root.get("bidCount"), 0));
            case CANCELED -> throw new IllegalStateException("CANCELED는 search()에서 이미 처리됨");
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

    private AuctionListSummary toSummary(AuctionSearchView v, AuctionStatus status) {
        return new AuctionListSummary(v.getAuctionId(), v.getProductId(), v.getTitle(),
                v.getArtistName(), v.getReleaseYear(), v.getGenre(), v.getPressType(), v.getThumbnail(),
                v.getSellerId(), v.getSellerNickname(), status, v.getHighestBidAmount(),
                v.getBidCount(), v.getStartAt(), v.getEndAt());
    }

    private Sort resolveSort(AuctionSortType sortType) {
        Sort primary = switch (sortType) {
            case PRICE_ASC -> Sort.by(Sort.Direction.ASC, "highestBidAmount");
            case PRICE_DESC -> Sort.by(Sort.Direction.DESC, "highestBidAmount");
            case MOST_BIDS -> Sort.by(Sort.Direction.DESC, "bidCount");
            case ENDING_SOON -> Sort.by(Sort.Direction.ASC, "endAt");   // 마감임박순 기본값
        };
        // 정렬 기준 값이 동일할 때 페이지 경계에서 순서가 흔들리지 않도록 auctionId(auto increment)를 tie-breaker로 사용
        return primary.and(Sort.by(Sort.Direction.ASC, "auctionId"));
    }

    @Override
    public void updateOnBid(Long auctionId, BigDecimal highestBidAmount, int bidCount,
                            LocalDateTime endAt) {
        AuctionSearchView view = searchViewJpaRepository.findById(auctionId).orElseThrow(
                () -> new AuctionException(AuctionErrorCode.AUCTION_SEARCH_VIEW_NOT_FOUND));
        view.updateOnBid(highestBidAmount, bidCount, endAt);
    }

}
