package site.productservice.application.dto.wishlist;

import site.productservice.application.dto.ProductSnapshotResult;

public record WishlistProductResult(
    Long productId,
    String title,
    String artistName,
    String genre,
    String label,
    Integer releaseYear,
    String releaseCountry,
    String pressType
) {

    public static WishlistProductResult of(final ProductSnapshotResult product) {
        return new WishlistProductResult(
            product.productId(),
            product.title(),
            product.artistName(),
            product.genre(),
            product.label(),
            product.releaseYear(),
            product.releaseCountry(),
            product.pressType() == null ? null : product.pressType().name()
        );
    }
}
