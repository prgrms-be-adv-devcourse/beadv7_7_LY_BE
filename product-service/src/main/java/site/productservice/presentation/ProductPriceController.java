package site.productservice.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.productservice.application.price.PriceQueryService;
import site.productservice.presentation.dto.price.PriceSummaryResponse;
import site.productservice.presentation.dto.price.PriceTradesResponse;
import site.productservice.presentation.dto.price.RecentTradesResponse;

/** 시세 조회 API (요약 3-1 · 추이 3-2 · 전역 최근 낙찰). 카탈로그(ProductController)와 관심사를 분리한 시세 전용 컨트롤러. */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductPriceController {

    private final PriceQueryService priceQueryService;

    @GetMapping("/{productId}/price-summary")
    public ApiResponse<PriceSummaryResponse> getPriceSummary(@PathVariable Long productId) {
        return ApiResponse.success(PriceSummaryResponse.from(priceQueryService.getPriceSummary(productId)));
    }

    @GetMapping("/{productId}/price-trades")
    public ApiResponse<PriceTradesResponse> getPriceTrades(@PathVariable Long productId) {
        return ApiResponse.success(PriceTradesResponse.from(priceQueryService.getPriceTrades(productId)));
    }

    /** 전역 최근 낙찰 — 게이트웨이가 products 하위 경로만 이 서비스로 라우팅하므로 별도 루트 대신 여기에 둔다. */
    @GetMapping("/price-trades/recent")
    public ApiResponse<RecentTradesResponse> getRecentTrades(@RequestParam(defaultValue = "6") int size) {
        return ApiResponse.success(RecentTradesResponse.from(priceQueryService.getRecentTrades(size)));
    }
}
