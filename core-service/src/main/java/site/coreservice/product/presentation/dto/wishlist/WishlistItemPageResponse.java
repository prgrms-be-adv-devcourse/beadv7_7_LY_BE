package site.coreservice.product.presentation.dto.wishlist;

import java.util.List;
import site.coreservice.product.application.dto.wishlist.WishlistItemPageResult;

public record WishlistItemPageResponse(List<WishlistItemDetailResponse> content, Long nextCursor,
                                       boolean hasNext) {

    public static WishlistItemPageResponse from(WishlistItemPageResult result) {
        List<WishlistItemDetailResponse> responses = result.content().stream()
            .map(WishlistItemDetailResponse::from)
            .toList();
        return new WishlistItemPageResponse(responses, result.nextCursor(), result.hasNext());
    }
}
