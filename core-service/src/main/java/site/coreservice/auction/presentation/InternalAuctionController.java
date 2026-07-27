package site.coreservice.auction.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import site.common.response.ApiResponse;
import site.coreservice.auction.application.InternalAuctionService;
import site.coreservice.auction.presentation.dto.InternalAuctionCountResponse;
import site.coreservice.auction.presentation.dto.InternalAuctionSummaryResponse;

import java.util.List;

@RestController
@RequestMapping("/internal/v1/auctions")
@RequiredArgsConstructor
public class InternalAuctionController {

    private final InternalAuctionService internalAuctionService;

    @GetMapping("/{auctionId}")
    public ApiResponse<InternalAuctionSummaryResponse> getAuction(@PathVariable Long auctionId) {
        return ApiResponse.success(InternalAuctionSummaryResponse.from(internalAuctionService.getInternalSummary(auctionId)));
    }

    @GetMapping("/counts")
    public ApiResponse<InternalAuctionCountResponse> getAuctionCounts(@RequestParam List<Long> productIds) {
        return ApiResponse.success(InternalAuctionCountResponse.from(internalAuctionService.getOpenAuctionCounts(productIds)));
    }

}
