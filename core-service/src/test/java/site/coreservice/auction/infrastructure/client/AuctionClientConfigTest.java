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
        RestClient productRestClient() {
            return RestClient.builder().baseUrl("http://other-domain").build();
        }
    }

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    AuctionClientConfig.class,
                    ConflictingRestClientConfig.class,
                    MemberHttpClient.class,
                    ProductHttpClient.class
            );

    @Test
    @DisplayName("RestClient 타입 빈이 여러 개 있어도 @Qualifier(auctionRestClient) 덕분에 충돌 없이 주입된다")
    void beansWireWithoutAmbiguity_whenAnotherRestClientBeanExists() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(MemberHttpClient.class);
            assertThat(context).hasSingleBean(ProductHttpClient.class);

            RestClient auctionRestClient = context.getBean("auctionRestClient", RestClient.class);
            MemberHttpClient memberHttpClient = context.getBean(MemberHttpClient.class);
            ProductHttpClient productHttpClient = context.getBean(ProductHttpClient.class);

            assertThat(getInjectedRestClient(memberHttpClient)).isSameAs(auctionRestClient);
            assertThat(getInjectedRestClient(productHttpClient)).isSameAs(auctionRestClient);
        });
    }

    private RestClient getInjectedRestClient(Object client) {
        return (RestClient) org.springframework.test.util.ReflectionTestUtils.getField(client, "auctionRestClient");
    }
}
