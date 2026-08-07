package site.auctionservice.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import site.common.response.ApiResponse;
import site.auctionservice.application.InternalAuctionService;
import site.auctionservice.presentation.dto.InternalAuctionCountResponse;
import site.auctionservice.presentation.dto.InternalAuctionSummaryResponse;

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
