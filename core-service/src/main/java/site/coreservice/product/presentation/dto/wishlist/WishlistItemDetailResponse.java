package site.coreservice.product.presentation.dto.wishlist;

import site.coreservice.product.application.dto.wishlist.WishlistItemResult;

public record WishlistItemDetailResponse(
    Long id,
    Long productId,
    String title,
    String artistName,
    String coverImageUrl,
    int releaseYear
) {

    public static WishlistItemDetailResponse from(WishlistItemResult result) {
        return new WishlistItemDetailResponse(
            result.id(),
            result.productId(),
            result.title(),
            result.artistName(),
            result.coverImageUrl(),
            result.releaseYear()
        );
    }
}
