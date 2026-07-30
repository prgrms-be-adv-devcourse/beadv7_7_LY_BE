package site.coreservice.auction.infrastructure.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class AuctionClientConfigTest {

    /**
     * order 도메인의 OrderClientConfig처럼, 동일 타입(RestClient)의 빈을 하나 더 등록해
     * 실제 충돌 상황을 재현한다.
     */
    @Configuration
    static class ConflictingRestClientConfig {
        @Bean
        RestClient someOtherDomainRestClient() {
            return RestClient.builder().baseUrl("http://other-domain").build();
        }
    }

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    AuctionClientConfig.class,
                    ConflictingRestClientConfig.class,
                    MemberHttpClient.class,
                    ProductHttpClient.class,
                    WalletHttpClient.class
            );

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

    private RestClient getInjectedRestClient(Object client, String fieldName) {
        return (RestClient) org.springframework.test.util.ReflectionTestUtils.getField(client, fieldName);
    }
}
