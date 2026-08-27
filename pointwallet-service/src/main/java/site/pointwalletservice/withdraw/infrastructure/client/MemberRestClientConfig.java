package site.pointwalletservice.withdraw.infrastructure.client;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class MemberRestClientConfig {

    // RestClient.builder()로 직접 생성하면 Boot가 관측/트레이싱을 붙여둔 오토컨피규어드 RestClient.Builder를
    // 우회하게 되어 W3C traceparent 헤더가 하위 서비스 호출에 전파되지 않는다.
    // 타임아웃 설정 이유 - Deposit의 TossPaymentGatewayAdapter(RestClientConfig)와 동일한 원칙: 지갑
    // 커넥션 풀이 20개뿐인 공유 MySQL 환경에서, member-service가 응답을 지연시키면 이 커넥션이
    // 그만큼 오래 묶여 다른 서비스에도 영향을 줄 수 있다. 계좌 조회는 별도 재시도 없이 1회만 부르므로
    // 타임아웃만으로도 충분한 방어가 된다.
    @Bean
    public RestClient memberRestClient(RestClient.Builder builder, @Value("${member.service.base-url}") String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(8));

        return builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }
}