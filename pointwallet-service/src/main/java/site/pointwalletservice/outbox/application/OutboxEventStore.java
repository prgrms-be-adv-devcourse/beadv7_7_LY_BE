// outbox/application/OutboxEventStore.java
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