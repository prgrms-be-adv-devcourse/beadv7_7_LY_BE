package site.explorationservice.recommendation.infrastructure.client;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import site.common.client.RestClientTemplate;
import site.common.response.ApiResponse;
import site.explorationservice.recommendation.application.port.WishlistPort;
import site.explorationservice.recommendation.application.port.dto.WishlistProduct;
import site.explorationservice.recommendation.exception.RecommendationErrorCode;
import site.explorationservice.recommendation.exception.RecommendationException;

@Slf4j
@Component
public class WishlistHttpClient implements WishlistPort {

    private final RestClient recommendationWishlistRestClient;

    public WishlistHttpClient(
        @Qualifier("recommendationWishlistRestClient") final RestClient recommendationWishlistRestClient) {
        this.recommendationWishlistRestClient = recommendationWishlistRestClient;
    }

    @Override
    public List<WishlistProduct> findRecentProducts(final Long memberId, final int limit) {
        final WishlistProductsPage page = RestClientTemplate.executeWithRetry(
            () -> recommendationWishlistRestClient.get()
                .uri("/internal/v1/members/{memberId}/liked-products?limit={limit}", memberId,
                    limit)
                .retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<WishlistProductsPage>>() {
                }),
            e -> handleError(e, memberId));

        return page.items();
    }

    /**
     * 4xx는 product-service가 우리 요청을 거절했다는 뜻이라 원인이 우리 쪽(요청 조립)에 있다고 본다 — 재시도해도 같은 요청을 또 보내는 것뿐이라 의미가
     * 없고, 우리 코드 결함일 가능성이 높아 error로 남긴다.
     * <p>
     * 5xx·연결 실패·응답은 왔지만 데이터가 빈 경우는 하나로 묶는다 — 셋 다 "product-service를 지금 신뢰할 수 없다"는 같은 결론이고, 일시적일 수 있어
     * warn으로 남긴다.
     */
    private RecommendationException handleError(final RestClientException e, final Long memberId) {
        if (e instanceof final HttpClientErrorException clientError) {
            log.error("위시리스트 조회 요청이 거절됐습니다 — memberId: {}, 상태: {}", memberId,
                clientError.getStatusCode(), e);
            return new RecommendationException(RecommendationErrorCode.WISHLIST_REQUEST_INVALID,
                "위시리스트 조회 요청이 올바르지 않습니다 — memberId: " + memberId, e);
        }

        log.warn("위시리스트 조회 실패(업스트림) — memberId: {}", memberId, e);
        return new RecommendationException(RecommendationErrorCode.WISHLIST_LOOKUP_FAILED,
            "위시리스트 조회 실패 — memberId: " + memberId, e);
    }

    private record WishlistProductsPage(List<WishlistProduct> items) {

    }
}
