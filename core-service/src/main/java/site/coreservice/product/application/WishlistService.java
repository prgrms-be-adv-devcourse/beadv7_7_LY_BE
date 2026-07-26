package site.coreservice.product.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.coreservice.product.application.dto.ProductSnapshotResult;
import site.coreservice.product.application.dto.WishlistItemPageResult;
import site.coreservice.product.application.dto.WishlistItemResult;
import site.coreservice.product.domain.WishlistItem;
import site.coreservice.product.domain.WishlistItemRepository;
import site.coreservice.product.exception.ProductNotFoundException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WishlistService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final WishlistItemRepository wishlistItemRepository;
    private final ProductService productService;

    @Transactional(readOnly = true)
    public WishlistItemPageResult findPage(final Long memberId, final Long cursor, final int size) {
        final RawPage rawPage = fetchRawPage(memberId, cursor, clampSize(size));
        final Map<Long, ProductSnapshotResult> productsById = fetchProductsById(rawPage.items());
        final List<WishlistItemResult> content = toResults(memberId, rawPage.items(), productsById);

        return new WishlistItemPageResult(content, rawPage.nextCursor(), rawPage.hasNext());
    }

    @Transactional
    public WishlistItem add(final Long memberId, final Long productId) {
        final ProductSnapshotResult product = productService.getProductSnapshot(productId);
        if (!product.active()) {
            throw new ProductNotFoundException();
        }
        return wishlistItemRepository.save(new WishlistItem(memberId, productId));
    }

    @Transactional
    public void remove(final Long memberId, final Long productId) {
        wishlistItemRepository.deleteByMemberIdAndProductId(memberId, productId);
    }

    private RawPage fetchRawPage(final Long memberId, final Long cursor, final int size) {
        final List<WishlistItem> fetched = wishlistItemRepository.findAllByMemberId(memberId,
            cursor, size + 1);
        final boolean hasNext = fetched.size() > size;
        final List<WishlistItem> items = hasNext ? fetched.subList(0, size) : fetched;
        final Long nextCursor = hasNext ? items.getLast().getId() : null;
        return new RawPage(items, nextCursor, hasNext);
    }

    private Map<Long, ProductSnapshotResult> fetchProductsById(
        final List<WishlistItem> wishlistItems) {
        final List<Long> productIds = wishlistItems.stream().map(WishlistItem::getProductId)
            .toList();
        return productService.getProductSnapshots(productIds).stream()
            .collect(Collectors.toMap(ProductSnapshotResult::productId, Function.identity()));
    }

    private List<WishlistItemResult> toResults(final Long memberId,
        final List<WishlistItem> wishlistItems,
        final Map<Long, ProductSnapshotResult> productsById) {
        return wishlistItems.stream()
            .filter(item -> hasActiveProduct(memberId, item, productsById))
            .map(item -> WishlistItemResult.of(item, productsById.get(item.getProductId())))
            .toList();
    }

    private int clampSize(final int size) {
        if (size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    // 상품이 아예 없거나(정합성 문제) 이후에 Soft Delete 됐으면 건너뛰고 로그를 남긴다.
    private boolean hasActiveProduct(final Long memberId, final WishlistItem item,
        final Map<Long, ProductSnapshotResult> productsById) {
        final ProductSnapshotResult product = productsById.get(item.getProductId());
        if (product == null) {
            log.error("wishlist: 참조하는 상품을 찾을 수 없어 건너뜁니다. memberId={}, productId={}",
                memberId, item.getProductId());
            return false;
        }
        if (!product.active()) {
            log.info("wishlist: 참조하는 상품이 비활성 상태라 건너뜁니다. memberId={}, productId={}",
                memberId, item.getProductId());
            return false;
        }
        return true;
    }

    private record RawPage(List<WishlistItem> items, Long nextCursor, boolean hasNext) {

    }
}
