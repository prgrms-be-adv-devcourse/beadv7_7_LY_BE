package site.productservice.presentation.dto.wishlist;

import java.util.List;
import site.productservice.application.dto.wishlist.WishlistItemPageResult;

public record WishlistItemPageResponse(List<WishlistItemDetailResponse> content, Long nextCursor,
                                       boolean hasNext) {

    public static WishlistItemPageResponse from(WishlistItemPageResult result) {
        List<WishlistItemDetailResponse> responses = result.content().stream()
            .map(WishlistItemDetailResponse::from)
            .toList();
        return new WishlistItemPageResponse(responses, result.nextCursor(), result.hasNext());
    }
}
