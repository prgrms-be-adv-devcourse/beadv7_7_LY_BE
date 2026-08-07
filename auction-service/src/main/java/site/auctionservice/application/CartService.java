package site.auctionservice.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.auctionservice.application.dto.CartItemGroupResult;
import site.auctionservice.application.dto.CartItemResult;
import site.auctionservice.application.dto.InternalAuctionSnapshotResult;
import site.auctionservice.domain.AuctionStatus;
import site.auctionservice.domain.CartItem;
import site.auctionservice.domain.CartItemRepository;
import site.auctionservice.exception.AuctionErrorCode;
import site.auctionservice.exception.AuctionException;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private static final int MAX_WATCHED_AUCTIONS = 30;

    // 결과 정렬 Comparator
    // 1. 경매 STATUS 우선순위: 진행중 -> 예정 -> 낙찰 -> 유찰 -> 취소
    // 2. RUNNING: 마감 가까운 순, SCHEDULED: 시작 가까운 순, ENDED_WON/ENDED_FAILED/CANCELED: 최근 종료 순
    private static final Comparator<CartItemResult> AUCTION_ORDER = Comparator
        .<CartItemResult, AuctionStatus>comparing(CartItemResult::status,
            Comparator.comparingInt(CartService::priorityOf))
        .thenComparing((a, b) -> switch (a.status()) {
            case RUNNING -> a.endAt().compareTo(b.endAt());
            case SCHEDULED -> a.startAt().compareTo(b.startAt());
            case ENDED_WON, ENDED_FAILED, CANCELED -> b.endAt().compareTo(a.endAt());
        });

    private final CartItemRepository cartItemRepository;
    private final InternalAuctionService internalAuctionService;

    @Transactional(readOnly = true)
    public List<CartItemGroupResult> findAll(final Long memberId) {
        final List<CartItem> cartItems = cartItemRepository.findAllByMemberId(memberId);

        if (cartItems.isEmpty()) {
            return List.of();
        }

        final Map<Long, InternalAuctionSnapshotResult> auctionsById = fetchAuctionsById(cartItems);
        final List<CartItemResult> sorted = toSortedResults(memberId, cartItems,
            auctionsById);
        return groupByStatus(sorted);
    }

    @Transactional
    public void addItem(final Long memberId, final Long auctionId) {
        if (cartItemRepository.existsByMemberIdAndAuctionId(memberId, auctionId)) {
            log.warn("watched-auctions: 이미 등록된 경매입니다. memberId={}, auctionId={}", memberId,
                auctionId);
            return;
        }
        validateCanAdd(memberId, auctionId);
        cartItemRepository.save(CartItem.of(memberId, auctionId));
    }

    @Transactional
    public void removeItem(final Long memberId, final Long auctionId) {
        cartItemRepository.deleteByMemberIdAndAuctionId(memberId, auctionId);
    }

    private Map<Long, InternalAuctionSnapshotResult> fetchAuctionsById(
        final List<CartItem> cartItems) {
        final List<Long> auctionIds = cartItems.stream().map(CartItem::getAuctionId).toList();
        return internalAuctionService.getSnapshots(auctionIds).stream()
            .collect(Collectors.toMap(InternalAuctionSnapshotResult::id, Function.identity()));
    }

    private List<CartItemResult> toSortedResults(final Long memberId,
        final List<CartItem> cartItems,
        final Map<Long, InternalAuctionSnapshotResult> auctionsById) {
        return cartItems.stream()
            .filter(cartItem -> hasAuction(memberId, cartItem, auctionsById))
            .map(cartItem -> CartItemResult.of(cartItem, auctionsById.get(cartItem.getAuctionId())))
            .sorted(AUCTION_ORDER)
            .toList();
    }

    // 경매는 Soft Delete라 일반적으로는 발생할 수 없는 상황이지만, 방어적으로 건너뛰고 로그를 남긴다.
    private boolean hasAuction(final Long memberId, final CartItem cartItem,
        final Map<Long, InternalAuctionSnapshotResult> auctionsById) {
        boolean found = auctionsById.containsKey(cartItem.getAuctionId());
        if (!found) {
            log.warn("watched-auctions: 참조하는 경매를 찾을 수 없어 건너뜁니다. memberId={}, auctionId={}",
                memberId, cartItem.getAuctionId());
        }
        return found;
    }

    private List<CartItemGroupResult> groupByStatus(final List<CartItemResult> sorted) {
        final Map<AuctionStatus, List<CartItemResult>> grouped = sorted.stream()
            .collect(Collectors.groupingBy(CartItemResult::status, LinkedHashMap::new,
                Collectors.toList()));

        return grouped.entrySet().stream()
            .map(entry -> CartItemGroupResult.of(entry.getKey(), entry.getValue()))
            .toList();
    }

    private void validateCanAdd(final Long memberId, final Long auctionId) {
        final InternalAuctionSnapshotResult auction = internalAuctionService.getSnapshot(auctionId);
        if (auction.status() != AuctionStatus.SCHEDULED
            && auction.status() != AuctionStatus.RUNNING) {
            throw new AuctionException(AuctionErrorCode.WATCHED_AUCTION_STATUS_INVALID);
        }
        if (cartItemRepository.countByMemberId(memberId) >= MAX_WATCHED_AUCTIONS) {
            throw new AuctionException(AuctionErrorCode.WATCHED_AUCTION_LIMIT_EXCEEDED);
        }
    }

    // 그룹 표시 우선순위: 진행중 -> 예정 -> 낙찰 -> 유찰 -> 취소
    private static int priorityOf(final AuctionStatus status) {
        return switch (status) {
            case RUNNING -> 0;
            case SCHEDULED -> 1;
            case ENDED_WON -> 2;
            case ENDED_FAILED -> 3;
            case CANCELED -> 4;
        };
    }
}
