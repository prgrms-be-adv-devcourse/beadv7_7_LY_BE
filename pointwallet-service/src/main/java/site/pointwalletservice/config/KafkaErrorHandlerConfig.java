package site.pointwalletservice.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import site.common.exception.BusinessException;

/**
 * 모든 @KafkaListener 컨테이너에 공통 적용되는 재시도 정책.
 * fulfillment-service와 동일한 정책 — BusinessException(홀드 없음 등 재시도해도
 * 의미없는 예외)은 재시도 제외, 그 외는 1초 간격 3회 재시도.
 * 재시도 소진 시 recoverer는 기본값(로그 후 스킵) — 알림/관측성은 이후 작업으로 미룸.
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