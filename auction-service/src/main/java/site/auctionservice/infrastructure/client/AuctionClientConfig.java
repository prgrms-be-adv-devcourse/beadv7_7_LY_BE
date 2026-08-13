package site.auctionservice.infrastructure.client;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AuctionClientConfig {

    @Bean
    RestClient auctionProductRestClient(@Value("${product.service.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }

    @Bean
    RestClient auctionWalletRestClient(@Value("${wallet.service.base-url}") String baseUrl, ClientHttpRequestFactory walletRequestFactory) {
        return RestClient.builder().baseUrl(baseUrl).requestFactory(walletRequestFactory).build();
    }

    @Bean
    RestClient auctionMemberRestClient(@Value("${member.service.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }

    @Bean
    ClientHttpRequestFactory walletRequestFactory(@Value("${wallet.service.pool.max-connections}") int maxConnections) {
        // TODO(#214): wallet.service.pool.max-connections(application.yml/application-local.yml)는 임시값(10).
        // 서비스 전체 커넥션 풀을 모듈별로 얼마나 배분할지는 회의에서 확정 예정 — 배분 결과가 나오면 yml 값만 바꾸면 된다(코드 변경 불필요).
        PoolingHttpClientConnectionManager pool =
                PoolingHttpClientConnectionManagerBuilder.create()
                        .setMaxConnTotal(maxConnections)
                        .setMaxConnPerRoute(maxConnections)
                        .setDefaultConnectionConfig(ConnectionConfig.custom()
                                .setConnectTimeout(Timeout.ofMilliseconds(300))
                                .setSocketTimeout(Timeout.ofMilliseconds(800))
                                .build())
                        .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(pool)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectionRequestTimeout(Timeout.ofMilliseconds(200))
                        .setResponseTimeout(Timeout.ofMilliseconds(800))
                        .build())
                .disableAutomaticRetries()
                .build();

        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }
}
