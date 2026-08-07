package site.productservice.application.dto.wishlist;

import site.productservice.application.dto.ProductSnapshotResult;
import site.productservice.domain.wishlist.WishlistItem;

public record WishlistItemResult(
    Long id,
    Long productId,
    String title,
    String artistName,
    String coverImageUrl,
    int releaseYear
) {

    public static WishlistItemResult of(WishlistItem wishlistItem, ProductSnapshotResult product) {
        return new WishlistItemResult(
            wishlistItem.getId(),
            wishlistItem.getProductId(),
            product.title(),
            product.artistName(),
            product.coverImageUrl(),
            product.releaseYear()
        );
    }
}
