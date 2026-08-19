package site.pointwalletservice.outbox.application;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import site.pointwalletservice.outbox.domain.OutboxEvent;
import site.pointwalletservice.outbox.domain.OutboxEventRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring Boot 4는 Jackson 3가 기본이라 ObjectMapper(Jackson 2) 대신 JsonMapper(Jackson 3)를 쓴다 -
 * Boot가 spring-boot-starter-webmvc를 통해 JsonMapper 빈을 자동 설정해주므로 별도 빈 등록 없이
 * 바로 주입받는다. Jackson 3의 예외(JacksonException)는 RuntimeException이라 Jackson 2 때와
 * 달리 catch가 필수는 아니지만, 직렬화 실패를 우리 쪽 예외 타입으로 감싸 의미를 명확히 하려고
 * 그대로 잡는다.
 * <p>
 * OutboxEventRepositoryImpl(인프라 계층)과 역할을 나눈 이유: Repository는 OutboxEvent 엔티티에
 * 대한 순수 CRUD만 담당하는 포트/어댑터고, 이 클래스는 애플리케이션 계층에서 "도메인 이벤트 객체
 * → JSON 직렬화 → OutboxEvent 생성 → 저장"까지를 한 번에 처리하는 파사드다. WithdrawApplicationService
 * 같은 호출부는 직렬화 로직을 몰라도 되고, 트랜잭션 스크립트 안에서 store(topic, key, event) 한 줄만
 * 호출하면 된다. 즉 Repository=영속성, Store=영속성+직렬화라는 책임 차이다.
 */
@Component
@RequiredArgsConstructor
public class OutboxEventStore {

    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper jsonMapper;

    public void store(String topic, String partitionKey, Object event) {
        String payload;
        try {
            payload = jsonMapper.writeValueAsString(event);
        } catch (JacksonException e) {
            throw new IllegalStateException("Outbox 이벤트 직렬화 실패: " + event.getClass().getName(), e);
        }
        outboxEventRepository.save(
                OutboxEvent.create(topic, partitionKey, event.getClass().getName(), payload)
        );
    }
}