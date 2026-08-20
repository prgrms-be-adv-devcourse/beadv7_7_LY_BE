package site.auctionservice.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.auctionservice.application.AuctionImageService;
import site.auctionservice.presentation.dto.AuctionImagePresignRequest;
import site.auctionservice.presentation.dto.AuctionImagePresignResponse;
import site.common.response.ApiResponse;
import site.common.web.MemberId;

@RestController
@RequestMapping("/api/v1/auctions")
@RequiredArgsConstructor
public class AuctionImageController {

    private final AuctionImageService auctionImageService;

    /** 경매 사진 업로드 주소 발급. 파일 실물은 브라우저가 발급받은 주소로 S3에 직접 올린다 —
     *  등록 요청에는 여기서 받은 imageUrl만 담는다. */
    @PostMapping("/images/presign")
    public ApiResponse<AuctionImagePresignResponse> presignImages(
            @RequestBody AuctionImagePresignRequest request, @MemberId Long memberId) {
        return ApiResponse.success(
                AuctionImagePresignResponse.from(auctionImageService.issueUploadUrls(request.toCommands())));
    }
}
