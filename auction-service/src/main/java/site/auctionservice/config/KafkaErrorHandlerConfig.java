package site.auctionservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import site.common.exception.BusinessException;

/**
 * 모든 @KafkaListener 컨테이너에 공통 적용되는 재시도 정책. Boot가 자동설정하는 ConcurrentKafkaListenerContainerFactory가 이
 * 빈을 자동으로 집어서 쓴다 — 리스너마다 따로 설정할 필요 없음.
 * <p>
 * 재시도 가능/불가능 분류는 지금은 이 한 줄(BusinessException은 재시도 안 함)뿐이다. 리스너마다 실제로 어떤 예외가 재시도해서 의미 있는지는 각 담당 도메인이
 * 더 알기 때문에, 필요해지면 addNotRetryableExceptions(...)에 항목을 추가하는 식으로 여기에 얹으면 된다 — 지금은 그 확장이 가능한 구조만 만들어 둔
 * 것.
 * <p>
 * recoverer(재시도 다 소진됐을 때 뭘 할지)는 아직 기본값(로그 후 스킵)이다. DLQ는 별도로 붙일 때 DeadLetterPublishingRecoverer를 두
 * 번째 생성자 인자로 넣으면 된다 — 이후 작업.
 */
@Configuration
public class KafkaErrorHandlerConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(new FixedBackOff(1_000L, 3L));
        errorHandler.addNotRetryableExceptions(BusinessException.class);
        return errorHandler;
    }
}
