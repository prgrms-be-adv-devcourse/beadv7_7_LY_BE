package site.productservice.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.common.web.MemberId;
import site.productservice.application.wishlist.WishlistServiceFacade;
import site.productservice.domain.wishlist.WishlistItem;
import site.productservice.presentation.dto.wishlist.WishlistItemPageResponse;

@RestController
@RequestMapping("/api/v1/members/me/liked-products")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistServiceFacade wishlistServiceFacade;

    @PutMapping("/{productId}")
    public ApiResponse<WishlistItemResponse> add(
        @MemberId final Long memberId,
        @PathVariable final Long productId
    ) {
        final WishlistItem saved = wishlistServiceFacade.add(memberId, productId);
        return ApiResponse.success(WishlistItemResponse.from(saved));
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Void> remove(
        @MemberId final Long memberId,
        @PathVariable final Long productId
    ) {
        wishlistServiceFacade.remove(memberId, productId);
        return ApiResponse.success();
    }

    @GetMapping
    public ApiResponse<WishlistItemPageResponse> findAll(
        @MemberId final Long memberId,
        @RequestParam(required = false) final Long cursor,
        @RequestParam(defaultValue = "20") final int size
    ) {
        return ApiResponse.success(WishlistItemPageResponse.from(
            wishlistServiceFacade.findPage(memberId, cursor, size)));
    }

    private record WishlistItemResponse(Long id, Long productId) {

        static WishlistItemResponse from(final WishlistItem wishlistItem) {
            return new WishlistItemResponse(wishlistItem.getId(), wishlistItem.getProductId());
        }
    }
}
