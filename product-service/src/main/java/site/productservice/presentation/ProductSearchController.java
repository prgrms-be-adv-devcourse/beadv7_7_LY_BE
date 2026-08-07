package site.productservice.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.productservice.application.search.ProductSearchService;
import site.productservice.presentation.dto.search.ProductSearchResponse;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class ProductSearchController {

    private final ProductSearchService productSearchService;

    /** q를 required=true로 두면 프레임워크 예외가 500으로 새는 기존 구멍이 있어, 검증은 서비스에서 한다. */
    @GetMapping("/products")
    public ApiResponse<ProductSearchResponse> searchProducts(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(ProductSearchResponse.from(productSearchService.searchProducts(q, page, size)));
    }
}
