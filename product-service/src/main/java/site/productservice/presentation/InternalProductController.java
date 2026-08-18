package site.productservice.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.productservice.application.ProductService;
import site.productservice.presentation.dto.ProductPageResponse;
import site.productservice.presentation.dto.ProductSnapshotResponse;

/**
 * 내부 시스템용 상품 API. 같은 core-service 안에서는 ProductService 직접 호출이 정식 창구다.
 */
@RestController
@RequestMapping("/internal/v1/products")
@RequiredArgsConstructor
public class InternalProductController {

    private final ProductService productService;

    @GetMapping("/{productId}/snapshot")
    public ApiResponse<ProductSnapshotResponse> getProductSnapshot(@PathVariable Long productId) {
        return ApiResponse.success(
            ProductSnapshotResponse.from(productService.getProductSnapshot(productId)));
    }

    /**
     * id 오름차순 전체 순회용. exploration-service의 상품 백필이 소비한다 — cursor 없이 처음 호출하고, 응답의 nextCursor를 다음 요청에
     * 그대로 넣는 식으로 hasNext가 false가 될 때까지 반복한다.
     */
    @GetMapping
    public ApiResponse<ProductPageResponse> getProductPage(
        @RequestParam(required = false) Long cursor,
        @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.success(
            ProductPageResponse.from(productService.getProductPage(cursor, limit)));
    }
}
