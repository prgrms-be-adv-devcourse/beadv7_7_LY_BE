package site.coreservice.product.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.coreservice.product.application.PriceQueryService;
import site.coreservice.product.presentation.dto.PriceSummaryResponse;
import site.coreservice.product.presentation.dto.PriceTradesResponse;

/** 시세 조회 API (요약 3-1 · 추이 3-2). 카탈로그(ProductController)와 관심사를 분리한 시세 전용 컨트롤러. */
@RestController
@RequestMapping("/api/v1/products/{productId}")
@RequiredArgsConstructor
public class ProductPriceController {

    private final PriceQueryService priceQueryService;

    @GetMapping("/price-summary")
    public ApiResponse<PriceSummaryResponse> getPriceSummary(@PathVariable Long productId) {
        return ApiResponse.success(PriceSummaryResponse.from(priceQueryService.getPriceSummary(productId)));
    }

    @GetMapping("/price-trades")
    public ApiResponse<PriceTradesResponse> getPriceTrades(@PathVariable Long productId) {
        return ApiResponse.success(PriceTradesResponse.from(priceQueryService.getPriceTrades(productId)));
    }
}
