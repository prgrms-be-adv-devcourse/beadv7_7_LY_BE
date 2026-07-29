package site.coreservice.pointwallet.withdraw.infrastructure.client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class MemberRestClientConfig {

    @Bean
    public RestClient memberRestClient(@Value("${member.service.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }
}