package site.coreservice.product.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.coreservice.product.application.ProductService;
import site.coreservice.product.presentation.dto.ProductSnapshotResponse;

/** 내부 시스템용 상품 API. 같은 core-service 안에서는 ProductService 직접 호출이 정식 창구다. */
@RestController
@RequestMapping("/internal/v1/products")
@RequiredArgsConstructor
public class InternalProductController {

    private final ProductService productService;

    @GetMapping("/{productId}/snapshot")
    public ApiResponse<ProductSnapshotResponse> getProductSnapshot(@PathVariable Long productId) {
        return ApiResponse.success(ProductSnapshotResponse.from(productService.getProductSnapshot(productId)));
    }
}
