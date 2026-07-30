package site.coreservice.auction.infrastructure.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import site.common.response.ApiResponse;
import site.coreservice.auction.application.port.ProductPort;
import site.coreservice.auction.application.port.dto.ProductDetail;
import site.coreservice.auction.application.port.dto.ProductSnapshot;
import site.coreservice.auction.exception.UpstreamContractViolationException;

/**
 *  product 도메인 API가 아직 개발/검증 완료되지 않아 API 명세서 기준으로 우선 작성함. 명세 변경 시 수정될 수 있음.
 *  로컬에서는 MockProductClient로 대체하고, 추후 실제 API 확정되면 이 클라이언트로 교체 예정.
 */
@Component
public class ProductHttpClient implements ProductPort {

    private final RestClient auctionProductRestClient;

    public ProductHttpClient(@Qualifier("auctionProductRestClient") RestClient auctionProductRestClient) {
        this.auctionProductRestClient = auctionProductRestClient;
    }

    @Override
    public ProductSnapshot getProduct(Long productId) {
        return fetch(productId, new ParameterizedTypeReference<ApiResponse<ProductSnapshot>>() {});
    }

    @Override
    public ProductDetail getProductDetail(Long productId) {
        return fetch(productId, new ParameterizedTypeReference<ApiResponse<ProductDetail>>() {});
    }

    private <T> T fetch(Long productId, ParameterizedTypeReference<ApiResponse<T>> typeReference) {
        ApiResponse<T> body;
        try {
            body = auctionProductRestClient.get()
                    .uri("/internal/v1/products/{productId}/snapshot", productId)
                    .retrieve()
                    .body(typeReference);
        } catch (HttpClientErrorException e) {
            throw new UpstreamContractViolationException(
                    "상품 조회 실패 — productId: " + productId + ", 상태: " + e.getStatusCode());
        }

        if (body == null || body.getData() == null) {
            throw new UpstreamContractViolationException("상품 조회 응답 본문이 비어 있습니다 — productId: " + productId);
        }
        return body.getData();
    }

}
