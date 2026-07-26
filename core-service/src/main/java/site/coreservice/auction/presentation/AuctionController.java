package site.coreservice.auction.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import site.common.response.ApiResponse;
import site.common.web.MemberId;
import site.coreservice.auction.application.AuctionService;
import site.coreservice.auction.presentation.dto.*;

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

    @GetMapping("/{auctionId}")
    public ApiResponse<AuctionDetailResponse> getAuctionDetail(
            @PathVariable Long auctionId,
            @RequestHeader(value = MemberId.HEADER_NAME, required = false) Long viewerId) {
        return ApiResponse.success(AuctionDetailResponse.from(auctionService.getAuctionDetail(auctionId, viewerId)));
    }

    @GetMapping("/participated")
    public ApiResponse<PageResponse<ParticipatedAuctionResponse>> participated(
            @MemberId Long bidderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(PageResponse.from(auctionService.getParticipatedAuctions(bidderId, pageable), ParticipatedAuctionResponse::from));
    }

}
