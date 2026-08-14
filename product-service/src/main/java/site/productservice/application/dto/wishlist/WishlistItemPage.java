package site.productservice.application.dto.wishlist;

import java.util.List;
import site.productservice.domain.wishlist.WishlistItem;

/**
 * 위시리스트 한 페이지. 아직 상품 정보가 채워지기 전 단계라 위시리스트 항목만 들어 있다.
 * <p>
 * 상품 제목·이미지 등을 채우고 비활성 상품을 걸러낸 최종 형태는 {@link WishlistItemPageResult}다.
 */
public record WishlistItemPage(List<WishlistItem> items, Long nextCursor, boolean hasNext) {

}
