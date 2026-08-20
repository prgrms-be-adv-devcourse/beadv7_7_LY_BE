package site.pointwalletservice.deposit.infrastructure.config;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    // RestClient.builder()로 직접 생성하면 Boot가 관측/트레이싱을 붙여둔 오토컨피규어드 RestClient.Builder를
    // 우회하게 되어 W3C traceparent 헤더가 하위 서비스 호출에 전파되지 않는다.
    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(60));

        return builder
                .requestFactory(requestFactory)
                .build();
    }
}