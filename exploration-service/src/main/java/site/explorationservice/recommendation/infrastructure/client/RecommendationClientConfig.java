package site.explorationservice.recommendation.infrastructure.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RecommendationClientConfig {

    @Bean
    RestClient explorationWishlistRestClient(
        @Value("${exploration.wishlist.base-url}") final String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }
}
