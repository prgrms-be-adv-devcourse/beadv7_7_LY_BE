package site.coreservice.product.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.coreservice.product.application.ProductService;
import site.coreservice.product.presentation.dto.ProductDetailResponse;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/{productId}")
    public ApiResponse<ProductDetailResponse> getActiveProductDetail(@PathVariable Long productId) {
        return ApiResponse.success(ProductDetailResponse.from(productService.getActiveProductDetail(productId)));
    }
}
