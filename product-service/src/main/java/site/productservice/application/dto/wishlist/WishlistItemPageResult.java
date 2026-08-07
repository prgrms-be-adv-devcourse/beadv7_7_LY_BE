package site.productservice.application.dto.wishlist;

import java.util.List;

public record WishlistItemPageResult(List<WishlistItemResult> content, Long nextCursor,
                                     boolean hasNext) {

}
