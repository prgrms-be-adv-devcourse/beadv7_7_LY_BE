package site.coreservice.auction.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import site.common.response.ApiResponse;
import site.coreservice.auction.application.AuctionService;
import site.coreservice.auction.presentation.dto.AuctionCreateRequest;
import site.coreservice.auction.presentation.dto.AuctionResultResponse;

@RestController
@RequestMapping("/api/v1/auctions")
@RequiredArgsConstructor
public class AuctionController {
    private static final Long TEMP_MEMBER_ID = 1L; // TODO(#52) : 인증 붙으면 로그인 사용자 정보로 교체

    private final AuctionService auctionService;

    @PostMapping
    public ApiResponse<AuctionResultResponse> createAuction(@RequestBody AuctionCreateRequest auctionCreateRequest) {
        return ApiResponse.success(AuctionResultResponse.from(auctionService.createAuction(auctionCreateRequest.toCommand(), TEMP_MEMBER_ID)));
    }

}
