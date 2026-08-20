package site.explorationservice.productindex.infrastructure.client;

import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import site.common.client.RestClientTemplate;
import site.common.client.RetryPolicy;
import site.common.response.ApiResponse;
import site.explorationservice.productindex.application.port.ProductPort;
import site.explorationservice.productindex.application.port.dto.ProductPage;

@Slf4j
@Component
public class ProductHttpClient implements ProductPort {

    private final RestClient productIndexProductRestClient;

    public ProductHttpClient(
        @Qualifier("productIndexProductRestClient") final RestClient productIndexProductRestClient) {
        this.productIndexProductRestClient = productIndexProductRestClient;
    }

    @Override
    public ProductPage findAllProducts(final Long cursor, final int limit) {
        return RestClientTemplate.executeWithRetry(
            () -> productIndexProductRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/internal/v1/products")
                    .queryParamIfPresent("cursor", Optional.ofNullable(cursor))
                    .queryParam("limit", limit)
                    .build())
                .retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<ProductPage>>() {
                }),
            RetryPolicy.DEFAULT,
            e -> handleError(e, cursor));
    }

    /**
     * 4xx는 product-service가 우리 요청을 거절했다는 뜻이라 원인이 우리 쪽(요청 조립)에 있다고 본다. 5xx·연결 실패·빈 응답은 하나로 묶는다 — 백필
     * 도중이면 페이지 하나를 실패 목록에 남기고 다음 페이지로 넘어가는 쪽(ProductBackfillService)이 판단할 몫이라, 여기서는 종류만 구분해서 알린다.
     */
    private RestClientException handleError(final RestClientException e, final Long cursor) {
        if (e instanceof final HttpClientErrorException clientError) {
            log.error("상품 조회 요청이 거절됐습니다 — cursor: {}, 상태: {}", cursor,
                clientError.getStatusCode(), e);
            return e;
        }

        log.warn("상품 조회 실패(업스트림) — cursor: {}", cursor, e);
        return e;
    }
}
