package site.coreservice.product.application.dto;

import site.coreservice.product.domain.WishlistItem;

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
