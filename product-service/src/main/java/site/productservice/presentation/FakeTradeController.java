package site.productservice.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.productservice.application.price.FakeTradePublishService;
import site.productservice.presentation.dto.price.FakeTradeConfirmedRequest;

/**
 * 가짜 거래확정 발행 트리거 (local 전용 — 프로파일 밖에서는 빈이 뜨지 않아 경로 자체가 없다).
 * 같은 auctionId로 두 번 쏘면 두 번째는 건너뛰어지는 것을 로그로 확인할 수 있다.
 */
@Profile("local")
@ConditionalOnProperty(name = "product.fake-trade.enabled", havingValue = "true")
@RestController
@RequiredArgsConstructor
public class FakeTradeController {

    private final FakeTradePublishService fakeTradePublishService;

    @PostMapping("/internal/v1/fake-trade-confirmed")
    public ApiResponse<Void> publishFakeTradeConfirmed(@RequestBody FakeTradeConfirmedRequest request) {
        fakeTradePublishService.publishFakeTradeConfirmed(request.auctionId(), request.confirmedAt());
        return ApiResponse.success();
    }
}
