package site.coreservice.product.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.coreservice.global.web.MemberId;
import site.coreservice.product.application.WishlistItemService;
import site.coreservice.product.domain.WishlistItem;

import java.util.List;

@RestController
@RequestMapping("/api/v1/members/me/liked-products")
@RequiredArgsConstructor
public class WishlistItemController {

    private final WishlistItemService wishlistItemService;

    @PutMapping("/{productId}")
    public ApiResponse<WishlistItemResponse> add(
        @MemberId final Long memberId,
        @RequestBody final AddWishlistItemRequest request
    ) {
        final WishlistItem saved = wishlistItemService.add(memberId, request.productId());
        return ApiResponse.success(WishlistItemResponse.from(saved));
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Void> remove(
        @MemberId final Long memberId,
        @PathVariable final Long productId
    ) {
        wishlistItemService.remove(memberId, productId);
        return ApiResponse.success();
    }

    @GetMapping
    public ApiResponse<List<WishlistItemResponse>> findAll(
        @MemberId final Long memberId
    ) {
        final List<WishlistItemResponse> responses = wishlistItemService.findAll(memberId).stream()
            .map(WishlistItemResponse::from)
            .toList();
        return ApiResponse.success(responses);
    }

    record AddWishlistItemRequest(Long productId) {
    }

    record WishlistItemResponse(Long id, Long productId) {
        static WishlistItemResponse from(final WishlistItem wishlistItem) {
            return new WishlistItemResponse(wishlistItem.getId(), wishlistItem.getProductId());
        }
    }
}
