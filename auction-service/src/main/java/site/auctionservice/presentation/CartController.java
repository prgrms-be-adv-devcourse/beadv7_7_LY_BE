package site.auctionservice.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import site.common.response.ApiResponse;
import site.common.web.MemberId;
import site.auctionservice.application.CartService;
import site.auctionservice.presentation.dto.WatchedAuctionGroupResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/members/me/watched-auctions")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PutMapping("/{auctionId}")
    public ApiResponse<Void> watch(
        @MemberId final Long memberId,
        @PathVariable final Long auctionId
    ) {
        cartService.addItem(memberId, auctionId);
        return ApiResponse.success();
    }

    @DeleteMapping("/{auctionId}")
    public ApiResponse<Void> unwatch(
        @MemberId final Long memberId,
        @PathVariable final Long auctionId
    ) {
        cartService.removeItem(memberId, auctionId);
        return ApiResponse.success();
    }

    @GetMapping
    public ApiResponse<List<WatchedAuctionGroupResponse>> findAll(
        @MemberId final Long memberId
    ) {
        final List<WatchedAuctionGroupResponse> responses = cartService.findAll(memberId)
            .stream()
            .map(WatchedAuctionGroupResponse::from)
            .toList();
        return ApiResponse.success(responses);
    }
}
