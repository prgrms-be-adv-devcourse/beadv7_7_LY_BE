package site.coreservice.auction.infrastructure.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import site.coreservice.auction.application.port.dto.ProductDetail;
import site.coreservice.auction.application.port.dto.ProductSnapshot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ProductHttpClientTest {

    private MockRestServiceServer server;
    private ProductHttpClient productHttpClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8080");
        server = MockRestServiceServer.bindTo(builder).build();
        productHttpClient = new ProductHttpClient(builder.build());
    }

    @Test
    @DisplayName("product 서버 응답을 파싱해 상품 스냅샷을 반환한다")
    void testGetProduct_success_returnsSnapshot() {
        server.expect(requestTo("http://localhost:8080/internal/v1/products/100/snapshot"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "data": {
                            "productId": 100,
                            "title": "Test Album",
                            "artistName": "Test Artist",
                            "releaseYear": 2020,
                            "genre": "Rock",
                            "pressType": "LP",
                            "active": true
                          },
                          "error": {"code": null, "message": null}
                        }
                        """, MediaType.APPLICATION_JSON));

        ProductSnapshot snapshot = productHttpClient.getProduct(100L);

        assertThat(snapshot).isEqualTo(new ProductSnapshot(100L, "Test Album", "Test Artist", 2020, "Rock", "LP", true));
        server.verify();
    }

    @Test
    @DisplayName("product 서버 응답을 파싱해 상품 상세를 반환한다")
    void testGetProductDetail_success_returnsDetail() {
        server.expect(requestTo("http://localhost:8080/internal/v1/products/100/snapshot"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "data": {
                            "productId": 100,
                            "title": "Test Album",
                            "artistName": "Test Artist",
                            "coverImageUrl": "http://image.example.com/1.png",
                            "releaseYear": 2020,
                            "genre": "Rock",
                            "pressType": "LP",
                            "active": true
                          },
                          "error": {"code": null, "message": null}
                        }
                        """, MediaType.APPLICATION_JSON));

        ProductDetail detail = productHttpClient.getProductDetail(100L);

        assertThat(detail).isEqualTo(new ProductDetail(100L, "Test Album", "Test Artist", "http://image.example.com/1.png", 2020, "Rock", "LP", true));
        server.verify();
    }
}
