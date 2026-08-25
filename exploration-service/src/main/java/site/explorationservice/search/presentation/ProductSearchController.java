package site.explorationservice.search.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.explorationservice.search.application.ProductSearchService;
import site.explorationservice.search.presentation.dto.ProductSearchResponse;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class ProductSearchController {

    private final ProductSearchService productSearchService;

    /**
     * q를 required=true로 두면 프레임워크가 던지는 예외가 500으로 새어 나간다. 검증은 서비스에서 해서
     * 400 + 정해진 에러코드로 나가게 한다.
     */
    @GetMapping("/products")
    public ApiResponse<ProductSearchResponse> searchProducts(
        @RequestParam(required = false) final String q,
        @RequestParam(defaultValue = "0") final int page,
        @RequestParam(defaultValue = "20") final int size) {
        return ApiResponse.success(
            ProductSearchResponse.from(productSearchService.searchProducts(q, page, size)));
    }
}
