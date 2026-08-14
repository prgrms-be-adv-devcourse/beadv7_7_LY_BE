package site.productservice.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.productservice.application.wishlist.WishlistServiceFacade;
import site.productservice.presentation.dto.wishlist.WishlistProductsResponse;

/**
 * 내부 시스템용 위시리스트 API. 추천(exploration-service)이 관심사 벡터를 만들 때 호출한다.
 */
@RestController
@RequestMapping("/internal/v1/members/{memberId}/liked-products")
@RequiredArgsConstructor
public class InternalWishlistController {

    private static final String DEFAULT_LIMIT = "50";

    private final WishlistServiceFacade wishlistServiceFacade;

    /**
     * limit 상한은 WishlistService가 건다(100건). 관심사를 파악하는 데 그 이상은 필요하지 않고, 담은 상품 전부를 긁어가면 소비하는 쪽에서 제외
     * 목록이 그만큼 커진다.
     */
    @GetMapping
    public ApiResponse<WishlistProductsResponse> findRecent(
        @PathVariable final Long memberId,
        @RequestParam(defaultValue = DEFAULT_LIMIT) final int limit
    ) {
        return ApiResponse.success(WishlistProductsResponse.from(
            wishlistServiceFacade.findRecentProducts(memberId, limit)));
    }
}
