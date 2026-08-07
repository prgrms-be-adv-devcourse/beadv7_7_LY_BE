package site.fulfillmentservice.order.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import site.fulfillmentservice.order.application.port.ProductInfo;

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
    @DisplayName("product 내부 API(/internal/v1/products/{id}/snapshot)를 호출해 상품 정보를 반환한다")
    void testGetProductInfo_success_returnsProductInfo() {
        server.expect(requestTo("http://localhost:8080/internal/v1/products/100/snapshot"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "data": {
                            "productId": 100,
                            "title": "Abbey Road",
                            "artistName": "The Beatles",
                            "releaseYear": 1969,
                            "pressType": "ORIGINAL",
                            "active": true
                          },
                          "error": {"code": null, "message": null}
                        }
                        """, MediaType.APPLICATION_JSON));

        ProductInfo info = productHttpClient.getProductInfo(100L);

        assertThat(info).isEqualTo(new ProductInfo("Abbey Road", "The Beatles", 1969, "ORIGINAL"));
        server.verify();
    }
}
