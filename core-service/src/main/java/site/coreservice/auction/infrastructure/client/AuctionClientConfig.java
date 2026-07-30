package site.coreservice.auction.infrastructure.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AuctionClientConfig {

    @Bean
    RestClient auctionProductRestClient(@Value("${auction.product-api.base-url:http://localhost:8080}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }

    @Bean
    RestClient auctionWalletRestClient(@Value("${auction.wallet-api.base-url:http://localhost:8080}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }

    @Bean
    RestClient auctionMemberRestClient(@Value("${auction.member-api.base-url:http://localhost:8081}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }

    @Bean
    @Qualifier("auctionMemberRestClient")
    RestClient auctionMemberRestClient(@Value("${member.service.base-url:http://localhost:81}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
