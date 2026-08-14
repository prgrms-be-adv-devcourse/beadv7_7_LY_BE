package site.productservice.presentation.dto.wishlist;

import java.util.List;
import site.productservice.application.dto.wishlist.WishlistProductResult;

/**
 * 내부 위시리스트 조회 응답. 추천이 관심사 벡터를 만들 때 쓴다.
 * <p>
 * 담는 필드가 화면용 응답({@link WishlistItemPageResponse})과 다르다 — 커버 이미지는 빠지고 레이블·발매 국가·프레스 타입이 들어간다. 읽는 쪽이
 * 이 값들을 <b>임베딩 텍스트의 재료</b>로 쓰기 때문이다. 같은 위시리스트를 읽지만 쓰임이 달라서, 공개 API를 재사용하지 않고 따로 두었다.
 * <p>
 * 커서·hasNext가 없는 건 읽는 쪽이 페이지를 넘기지 않기 때문이다.
 */
public record WishlistProductsResponse(List<Item> items) {

    public record Item(
        Long productId,
        String title,
        String artistName,
        String genre,
        String label,
        Integer releaseYear,
        String releaseCountry,
        String pressType
    ) {

        private static Item from(final WishlistProductResult product) {
            return new Item(
                product.productId(),
                product.title(),
                product.artistName(),
                product.genre(),
                product.label(),
                product.releaseYear(),
                product.releaseCountry(),
                product.pressType()
            );
        }
    }

    public static WishlistProductsResponse from(final List<WishlistProductResult> products) {
        return new WishlistProductsResponse(products.stream().map(Item::from).toList());
    }
}
