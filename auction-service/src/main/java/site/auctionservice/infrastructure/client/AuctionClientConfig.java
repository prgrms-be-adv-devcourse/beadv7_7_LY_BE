package site.auctionservice.infrastructure.client;

import java.time.Duration;
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
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AuctionClientConfig {

    @Bean
    RestClient auctionProductRestClient(
            @Value("${product.service.base-url}") String baseUrl,
            @Value("${product.service.connect-timeout-ms:500}") long connectTimeoutMs,
            @Value("${product.service.read-timeout-ms:1000}") long readTimeoutMs) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(timeoutRequestFactory(connectTimeoutMs, readTimeoutMs))
                .build();
    }

    @Bean
    RestClient auctionWalletRestClient(@Value("${wallet.service.base-url}") String baseUrl, ClientHttpRequestFactory walletRequestFactory) {
        return RestClient.builder().baseUrl(baseUrl).requestFactory(walletRequestFactory).build();
    }

    @Bean
    RestClient auctionMemberRestClient(
            @Value("${member.service.base-url}") String baseUrl,
            @Value("${member.service.connect-timeout-ms:300}") long connectTimeoutMs,
            @Value("${member.service.read-timeout-ms:500}") long readTimeoutMs) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(timeoutRequestFactory(connectTimeoutMs, readTimeoutMs))
                .build();
    }

    private SimpleClientHttpRequestFactory timeoutRequestFactory(long connectTimeoutMs, long readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return requestFactory;
    }

    @Bean
    ClientHttpRequestFactory walletRequestFactory(@Value("${wallet.service.pool.max-connections}") int maxConnections) {
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
