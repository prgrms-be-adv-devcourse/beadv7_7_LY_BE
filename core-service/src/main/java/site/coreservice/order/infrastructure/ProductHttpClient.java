package site.coreservice.order.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import site.common.response.ApiResponse;
import site.coreservice.order.application.port.ProductInfo;
import site.coreservice.order.application.port.ProductPort;

@Component("productHttpClientForOrder")
@RequiredArgsConstructor
public class ProductHttpClient implements ProductPort {

    private final RestClient productRestClient;

    @Override
    public ProductInfo getProductInfo(Long productId) {
        ApiResponse<ProductInfo> response = productRestClient.get()
                .uri("/internal/v1/products/{productId}", productId)
                .retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<ProductInfo>>() {
                });

        return response.getData();
    }
}
