package site.coreservice.auction.presentation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.coreservice.auction.domain.AuctionEventPublisher;

import java.math.BigDecimal;

// 작업 시 이벤트 발행용 임시 컨트롤러
@Slf4j
@RestController
@RequestMapping("/api/test/auctions")
public class AuctionEventTestController {

    private final AuctionEventPublisher auctionEventPublisher;

    public AuctionEventTestController(final AuctionEventPublisher auctionEventPublisher) {
        this.auctionEventPublisher = auctionEventPublisher;
    }

    @PostMapping("/won")
    public ApiResponse<AuctionWonTestRequest> publishWon(
        @RequestBody final AuctionWonTestRequest request) {
        auctionEventPublisher.publishWon(
            request.auctionId(),
            request.productId(),
            request.winnerId(),
            request.sellerId(),
            request.winningPrice()
        );
        return ApiResponse.success(request);
    }

    record AuctionWonTestRequest(
        Long auctionId,
        Long productId,
        Long winnerId,
        Long sellerId,
        BigDecimal winningPrice
    ) {

    }
}
