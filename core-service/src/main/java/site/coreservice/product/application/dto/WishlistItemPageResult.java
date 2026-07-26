package site.coreservice.product.application.dto;

import java.util.List;

public record WishlistItemPageResult(List<WishlistItemResult> content, Long nextCursor,
                                     boolean hasNext) {

}
