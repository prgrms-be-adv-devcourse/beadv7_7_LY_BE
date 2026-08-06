package site.coreservice.product.presentation;

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
import site.coreservice.product.application.wishlist.WishlistService;
import site.coreservice.product.domain.wishlist.WishlistItem;
import site.coreservice.product.presentation.dto.wishlist.WishlistItemPageResponse;

@RestController
@RequestMapping("/api/v1/members/me/liked-products")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @PutMapping("/{productId}")
    public ApiResponse<WishlistItemResponse> add(
        @MemberId final Long memberId,
        @PathVariable final Long productId
    ) {
        final WishlistItem saved = wishlistService.add(memberId, productId);
        return ApiResponse.success(WishlistItemResponse.from(saved));
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Void> remove(
        @MemberId final Long memberId,
        @PathVariable final Long productId
    ) {
        wishlistService.remove(memberId, productId);
        return ApiResponse.success();
    }

    @GetMapping
    public ApiResponse<WishlistItemPageResponse> findAll(
        @MemberId final Long memberId,
        @RequestParam(required = false) final Long cursor,
        @RequestParam(defaultValue = "20") final int size
    ) {
        return ApiResponse.success(WishlistItemPageResponse.from(
            wishlistService.findPage(memberId, cursor, size)));
    }

    private record WishlistItemResponse(Long id, Long productId) {

        static WishlistItemResponse from(final WishlistItem wishlistItem) {
            return new WishlistItemResponse(wishlistItem.getId(), wishlistItem.getProductId());
        }
    }
}
