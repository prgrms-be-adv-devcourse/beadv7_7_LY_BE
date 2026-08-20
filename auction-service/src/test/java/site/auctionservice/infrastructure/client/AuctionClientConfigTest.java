package site.auctionservice.infrastructure.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class AuctionClientConfigTest {

    @Configuration
    static class ConflictingRestClientConfig {
        @Bean
        RestClient someOtherDomainRestClient() {
            return RestClient.builder().baseUrl("http://other-domain").build();
        }
    }

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
            .withUserConfiguration(
                    AuctionClientConfig.class,
                    ConflictingRestClientConfig.class,
                    MemberHttpClient.class,
                    ProductHttpClient.class,
                    WalletHttpClient.class
            )
            .withPropertyValues("wallet.service.pool.max-connections=10");

    @Test
    @DisplayName("product/wallet/member 클라이언트가 각자 전용 RestClient 빈으로 주입된다")
    void eachClientWiresToItsOwnDedicatedRestClient() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(MemberHttpClient.class);
            assertThat(context).hasSingleBean(ProductHttpClient.class);
            assertThat(context).hasSingleBean(WalletHttpClient.class);

            RestClient productRestClient = context.getBean("auctionProductRestClient", RestClient.class);
            RestClient walletRestClient = context.getBean("auctionWalletRestClient", RestClient.class);
            RestClient memberRestClient = context.getBean("auctionMemberRestClient", RestClient.class);

            ProductHttpClient productHttpClient = context.getBean(ProductHttpClient.class);
            WalletHttpClient walletHttpClient = context.getBean(WalletHttpClient.class);
            MemberHttpClient memberHttpClient = context.getBean(MemberHttpClient.class);

            assertThat(getInjectedRestClient(productHttpClient, "auctionProductRestClient")).isSameAs(productRestClient);
            assertThat(getInjectedRestClient(walletHttpClient, "auctionWalletRestClient")).isSameAs(walletRestClient);
            assertThat(getInjectedRestClient(memberHttpClient, "auctionMemberRestClient")).isSameAs(memberRestClient);

            // 셋 다 서로 다른 인스턴스 — 다른 도메인 빈은 물론 auction 안에서도 용도별로 공유하지 않는다.
            assertThat(productRestClient).isNotSameAs(walletRestClient);
            assertThat(productRestClient).isNotSameAs(memberRestClient);
            assertThat(walletRestClient).isNotSameAs(memberRestClient);
        });
    }

    @Test
    @DisplayName("member/product RestClient은 무한 대기를 막는 connect/read timeout을 갖는다 - fallback이 없는 product보다 fallback이 있는 member를 더 짧게 잡는다")
    void memberAndProductRestClientsHaveTimeoutsConfigured() {
        contextRunner.run(context -> {
            RestClient productRestClient = context.getBean("auctionProductRestClient", RestClient.class);
            RestClient memberRestClient = context.getBean("auctionMemberRestClient", RestClient.class);

            assertTimeout(productRestClient, 500, 1000);
            assertTimeout(memberRestClient, 300, 500);
        });
    }

    private void assertTimeout(RestClient restClient, int expectedConnectTimeoutMs, int expectedReadTimeoutMs) {
        Object requestFactory = ReflectionTestUtils.getField(restClient, "clientRequestFactory");
        assertThat(requestFactory).isInstanceOf(SimpleClientHttpRequestFactory.class);
        assertThat((int) ReflectionTestUtils.getField(requestFactory, "connectTimeout")).isEqualTo(expectedConnectTimeoutMs);
        assertThat((int) ReflectionTestUtils.getField(requestFactory, "readTimeout")).isEqualTo(expectedReadTimeoutMs);
    }

    private RestClient getInjectedRestClient(Object client, String fieldName) {
        return (RestClient) org.springframework.test.util.ReflectionTestUtils.getField(client, fieldName);
    }
}
