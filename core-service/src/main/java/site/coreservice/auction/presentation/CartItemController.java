package site.coreservice.auction.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.common.web.MemberId;
import site.coreservice.auction.application.CartItemService;
import site.coreservice.auction.presentation.dto.WatchedAuctionGroupResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/members/me/watched-auctions")
@RequiredArgsConstructor
public class CartItemController {

    private final CartItemService cartItemService;

    @PutMapping("/{auctionId}")
    public ApiResponse<Void> watch(
        @MemberId final Long memberId,
        @PathVariable final Long auctionId
    ) {
        cartItemService.add(memberId, auctionId);
        return ApiResponse.success();
    }

    @DeleteMapping("/{auctionId}")
    public ApiResponse<Void> unwatch(
        @MemberId final Long memberId,
        @PathVariable final Long auctionId
    ) {
        cartItemService.remove(memberId, auctionId);
        return ApiResponse.success();
    }

    @GetMapping
    public ApiResponse<List<WatchedAuctionGroupResponse>> findAll(
        @MemberId final Long memberId
    ) {
        final List<WatchedAuctionGroupResponse> responses = cartItemService.findAll(memberId)
            .stream()
            .map(WatchedAuctionGroupResponse::from)
            .toList();
        return ApiResponse.success(responses);
    }
}
