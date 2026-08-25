package site.explorationservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import site.common.exception.BusinessException;

@Configuration
public class KafkaErrorHandlerConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(new FixedBackOff(1_000L, 3L));
        errorHandler.addNotRetryableExceptions(BusinessException.class);
        return errorHandler;
    }
}
