package site.coreservice.auction.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import site.common.response.ApiResponse;
import site.common.web.MemberId;
import site.coreservice.auction.application.AuctionService;
import site.coreservice.auction.presentation.dto.AuctionRequest;
import site.coreservice.auction.presentation.dto.AuctionResultResponse;

@RestController
@RequestMapping("/api/v1/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionService auctionService;

    @PostMapping
    public ApiResponse<AuctionResultResponse> createAuction(@RequestBody AuctionRequest auctionRequest, @MemberId Long sellerId) {
        return ApiResponse.success(AuctionResultResponse.from(auctionService.createAuction(auctionRequest.toCreateCommand(), sellerId)));
    }

    @PatchMapping("/{auctionId}")
    public ApiResponse<AuctionResultResponse> modifyAuction(@RequestBody AuctionRequest auctionRequest, @PathVariable Long auctionId, @MemberId Long sellerId) {
        return ApiResponse.success(AuctionResultResponse.from(auctionService.modifyAuction(auctionRequest.toModifyCommand(auctionId), sellerId)));
    }

    @DeleteMapping("/{auctionId}")
    public ApiResponse<Void> deleteAuction(@PathVariable Long auctionId, @MemberId Long sellerId) {
        auctionService.deleteAuction(auctionId, sellerId);
        return ApiResponse.success();
    }

}
