package site.explorationservice.productindex.infrastructure.client;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * ProductAuctionClientConfig(product-service)와 같은 패턴으로 타임아웃을 처음부터 건다 — 위시리스트 클라이언트에서는 이 부분이 빠졌던 걸
 * 뒤늦게 지적받았다. 백필은 이 클라이언트를 수백 번 반복 호출하므로, 상대가 응답 없이 물려버리면 그 영향이 한 번의 지연이 아니라 전체 백필 정지로 이어진다.
 */
@Configuration
public class ProductIndexClientConfig {

    @Bean
    RestClient productIndexProductRestClient(
        @Value("${exploration.product.base-url}") final String baseUrl,
        @Value("${exploration.product.connect-timeout-ms:1000}") final long connectTimeoutMs,
        @Value("${exploration.product.read-timeout-ms:2000}") final long readTimeoutMs) {
        final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        return RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }
}
