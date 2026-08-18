package site.explorationservice.recommendation.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import site.explorationservice.recommendation.application.port.dto.WishlistProduct;
import site.explorationservice.recommendation.exception.RecommendationErrorCode;
import site.explorationservice.recommendation.exception.RecommendationException;

/**
 * auction-service의 ProductHttpClientTest와 같은 패턴 — MockRestServiceServer로 실제 스프링 컨텍스트 없이 응답 파싱만
 * 검증한다.
 */
class WishlistHttpClientTest {

    private MockRestServiceServer server;
    private WishlistHttpClient wishlistHttpClient;

    @BeforeEach
    void setUp() {
        final RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8084");
        server = MockRestServiceServer.bindTo(builder).build();
        wishlistHttpClient = new WishlistHttpClient(builder.build());
    }

    @Test
    @DisplayName("응답의 items를 그대로 파싱해 돌려준다")
    void findRecentProducts_성공() {
        server.expect(
                requestTo("http://localhost:8084/internal/v1/members/1/liked-products?limit=50"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("""
                {
                  "success": true,
                  "data": {
                    "items": [
                      { "productId": 100, "title": "Kind of Blue", "artistName": "Miles Davis",
                        "genre": "Jazz", "label": "Columbia", "releaseYear": 1959,
                        "releaseCountry": "미국", "pressType": "ORIGINAL" }
                    ]
                  },
                  "error": {"code": null, "message": null}
                }
                """, MediaType.APPLICATION_JSON));

        final List<WishlistProduct> products = wishlistHttpClient.findRecentProducts(1L, 50);

        assertThat(products).containsExactly(
            new WishlistProduct(100L, "Kind of Blue", "Miles Davis", "Jazz", "Columbia", 1959,
                "미국", "ORIGINAL"));
        server.verify();
    }

    @Test
    @DisplayName("담은 상품이 없으면 빈 목록을 돌려준다")
    void findRecentProducts_빈_위시리스트() {
        server.expect(
                requestTo("http://localhost:8084/internal/v1/members/1/liked-products?limit=50"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("""
                { "success": true, "data": { "items": [] }, "error": {"code": null, "message": null} }
                """, MediaType.APPLICATION_JSON));

        assertThat(wishlistHttpClient.findRecentProducts(1L, 50)).isEmpty();
    }

    // RetryPolicy.DEFAULT(재시도 2회)가 붙어 있어 매번 실패하면 최초 시도 포함 3번 호출된다.
    // MockRestServiceServer는 expect()를 부른 순서대로 하나씩 소비하므로 세 번 다 걸어둬야 한다.
    @Test
    @DisplayName("5xx는 재시도해도 계속 실패하면 업스트림 실패로 분류한다")
    void findRecentProducts_업스트림_실패() {
        server.expect(
                requestTo("http://localhost:8084/internal/v1/members/1/liked-products?limit=50"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withServerError());
        server.expect(
                requestTo("http://localhost:8084/internal/v1/members/1/liked-products?limit=50"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withServerError());
        server.expect(
                requestTo("http://localhost:8084/internal/v1/members/1/liked-products?limit=50"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withServerError());

        assertThatThrownBy(() -> wishlistHttpClient.findRecentProducts(1L, 50))
            .isInstanceOf(RecommendationException.class)
            .extracting(e -> ((RecommendationException) e).getErrorCode())
            .isEqualTo(RecommendationErrorCode.WISHLIST_LOOKUP_FAILED);
        server.verify();
    }

    @Test
    @DisplayName("일시적인 5xx 뒤에 성공하면 재시도한 결과를 그대로 돌려준다")
    void findRecentProducts_재시도_후_성공() {
        server.expect(
                requestTo("http://localhost:8084/internal/v1/members/1/liked-products?limit=50"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withServerError());
        server.expect(
                requestTo("http://localhost:8084/internal/v1/members/1/liked-products?limit=50"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("""
                { "success": true, "data": { "items": [] }, "error": {"code": null, "message": null} }
                """, MediaType.APPLICATION_JSON));

        assertThat(wishlistHttpClient.findRecentProducts(1L, 50)).isEmpty();
        server.verify();
    }

    // 4xx는 우리가 잘못된 요청을 보냈다는 뜻이라 원인이 우리 쪽에 있다고 본다 — product-service 장애와
    // 다른 결론이라 별도 에러 코드로 갈린다.
    @Test
    @DisplayName("4xx는 우리 쪽 요청 문제로 분류한다")
    void findRecentProducts_잘못된_요청() {
        server.expect(
                requestTo("http://localhost:8084/internal/v1/members/1/liked-products?limit=50"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withBadRequest());

        assertThatThrownBy(() -> wishlistHttpClient.findRecentProducts(1L, 50))
            .isInstanceOf(RecommendationException.class)
            .extracting(e -> ((RecommendationException) e).getErrorCode())
            .isEqualTo(RecommendationErrorCode.WISHLIST_REQUEST_INVALID);
    }
}
