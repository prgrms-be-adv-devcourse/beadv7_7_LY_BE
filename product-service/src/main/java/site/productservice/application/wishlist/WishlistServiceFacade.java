package site.productservice.application.wishlist;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.productservice.application.ProductService;
import site.productservice.application.dto.ProductSnapshotResult;
import site.productservice.application.dto.wishlist.WishlistItemPage;
import site.productservice.application.dto.wishlist.WishlistItemPageResult;
import site.productservice.application.dto.wishlist.WishlistItemResult;
import site.productservice.application.dto.wishlist.WishlistProductResult;
import site.productservice.domain.wishlist.WishlistItem;
import site.productservice.exception.ProductNotFoundException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WishlistServiceFacade {

    private final WishlistService wishlistService;
    private final ProductService productService;
    private final WishlistEventPublisher wishlistEventPublisher;

    public WishlistItemPageResult findPage(final Long memberId, final Long cursor, final int size) {
        final WishlistItemPage page = wishlistService.findPage(memberId, cursor, size);
        final Map<Long, ProductSnapshotResult> productsById = fetchProductsById(page.items());
        final List<WishlistItemResult> content = toResults(memberId, page.items(), productsById);

        return new WishlistItemPageResult(content, page.nextCursor(), page.hasNext());
    }

    public List<WishlistProductResult> findRecentProducts(final Long memberId, final int limit) {
        final WishlistItemPage page = wishlistService.findPage(memberId, null, limit);
        final Map<Long, ProductSnapshotResult> productsById = fetchProductsById(page.items());

        return page.items().stream()
            .filter(item -> hasActiveProduct(memberId, item, productsById))
            .map(item -> WishlistProductResult.of(productsById.get(item.getProductId())))
            .toList();
    }

    public WishlistItemResult add(final Long memberId, final Long productId) {
        ProductSnapshotResult product = productService.getProductSnapshot(productId);
        if (!product.active()) {
            throw new ProductNotFoundException();
        }

        final WishlistItem saved = wishlistService.add(memberId, productId);
        wishlistEventPublisher.publishAdded(saved.getMemberId());
        return WishlistItemResult.of(saved, product);
    }

    // 위시리스트에 없던 상품을 삭제 요청해도 이벤트는 나간다.
    public void remove(final Long memberId, final Long productId) {
        wishlistService.remove(memberId, productId);
        wishlistEventPublisher.publishRemoved(memberId);
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
}
