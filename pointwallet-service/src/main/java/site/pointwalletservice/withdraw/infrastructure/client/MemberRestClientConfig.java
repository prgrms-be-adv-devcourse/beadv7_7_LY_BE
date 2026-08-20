package site.pointwalletservice.withdraw.infrastructure.client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class MemberRestClientConfig {

    // RestClient.builder()로 직접 생성하면 Boot가 관측/트레이싱을 붙여둔 오토컨피규어드 RestClient.Builder를
    // 우회하게 되어 W3C traceparent 헤더가 하위 서비스 호출에 전파되지 않는다.
    @Bean
    public RestClient memberRestClient(RestClient.Builder builder, @Value("${member.service.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }
}