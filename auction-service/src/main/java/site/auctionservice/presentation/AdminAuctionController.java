package site.auctionservice.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.auctionservice.application.AuctionService;
import site.common.response.ApiResponse;

@RestController
@RequestMapping("/api/admin/v1/auctions")
@RequiredArgsConstructor
public class AdminAuctionController {

    private final AuctionService auctionService;

    // TODO : #238 관리자 권한 관련 로직 추가
    @DeleteMapping("/{auctionId}")
    public ApiResponse<Void> forceCancel(@PathVariable Long auctionId) {
        auctionService.forceCancelAuction(auctionId);
        return ApiResponse.success();
    }
}
