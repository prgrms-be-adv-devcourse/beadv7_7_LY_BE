package site.coreservice.product.infrastructure.client;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * 상품 → 경매 호출용 클라이언트 설정. 경매 도메인이 이미 auctionRestClient라는 빈을 갖고 있어
 * 이름을 겹치지 않게 둔다. 특정 호출 전용이 아니라 상품→경매 공용이다 — 후속 경매 수 조회 연동이
 * 이 빈을 그대로 쓴다.
 * <p>
 * 타임아웃이 필수인 이유: 이 호출은 커밋 후 콜백 안에서 동기로 일어난다. 상한이 없으면 주문 확정
 * 응답이 무한정 늦어지고, 자동 완료 배치는 건마다 직렬로 밀린다. 상대가 같은 프로세스라 응답을
 * 기다리는 동안 웹 서버 스레드도 하나 더 물고 있다.
 */
@Configuration
public class ProductAuctionClientConfig {

    @Bean
    RestClient auctionApiRestClient(
            @Value("${product.auction-api.base-url:http://localhost:8080}") String baseUrl,
            @Value("${product.auction-api.connect-timeout-ms:2000}") long connectTimeoutMs,
            @Value("${product.auction-api.read-timeout-ms:3000}") long readTimeoutMs,
            JsonMapper jsonMapper) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        return configure(RestClient.builder(), baseUrl, jsonMapper)
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * 테스트가 운영 빈과 같은 경로로 조립하도록 떼어낸 부분. 여기서 요청 팩토리는 붙이지 않는다 —
     * 테스트가 목 서버를 물릴 때 갈아끼우는 자리라, 여기서 세팅하면 목이 지워진다.
     * <p>
     * JSON 컨버터를 우리 매퍼로 바꿔 다는 이유: 기본 컨버터는 자기 매퍼를 새로 만들어서, 나중에 팀이
     * 날짜 형식 같은 Jackson 설정을 바꾸면 내보내는 쪽만 바뀌고 읽는 쪽은 그대로 남아 조용히 어긋난다.
     * 나머지 기본 컨버터는 그대로 둔다 — 지금은 JSON만 주고받지만, 목록을 통째로 비워두면 나중에
     * 다른 응답 타입이 생겼을 때 원인을 찾기 어려운 형태로 깨진다.
     */
    public static RestClient.Builder configure(RestClient.Builder builder, String baseUrl, JsonMapper jsonMapper) {
        return builder
                .baseUrl(baseUrl)
                .configureMessageConverters(
                        converters -> converters.withJsonConverter(new JacksonJsonHttpMessageConverter(jsonMapper)));
    }
}
