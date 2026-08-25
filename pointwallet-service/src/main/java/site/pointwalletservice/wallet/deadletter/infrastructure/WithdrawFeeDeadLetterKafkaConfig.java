package site.pointwalletservice.wallet.deadletter.infrastructure;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import site.common.exception.BusinessException;

/**
 * WithdrawFeeEarnedEventListener(원본 리스너, wallet.infrastructure)에만 적용되는 전용
 * ContainerFactory. 공용 KafkaErrorHandlerConfig.kafkaErrorHandler()는 그대로 두고, 이
 * 리스너만 별도 팩토리(withdrawFeeKafkaListenerContainerFactory)를 쓰도록
 * @KafkaListener(containerFactory = ...)로 지정한다.
 * <p>
 * 이렇게 분리한 이유 - 공용 빈을 바꾸면 SettlementConfirmedEvent, OrderRefundedEvent 등
 * 다른 5개 리스너의 실패 처리 방식(재시도 소진 시 로그 후 스킵)까지 전부 DLQ 격리로
 * 한꺼번에 바뀌어버린다. 관리자 재처리까지 필요하다고 판단한 건 지금 WithdrawFeeEarnedEvent
 * 하나뿐이라, 그 판단이 다른 이벤트에 의도치 않게 전파되지 않도록 스코프를 좁혔다.
 * <p>
 * 재시도 정책(1초 간격 3회, BusinessException 제외) 자체는 공용 정책과 동일하게 맞췄다 -
 * 다른 건 recoverer(재시도 소진 후 처리)뿐이다.
 */
@Configuration
public class WithdrawFeeDeadLetterKafkaConfig {

    @Bean
    public DefaultErrorHandler withdrawFeeDeadLetterErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        DefaultErrorHandler errorHandler =
                new DefaultErrorHandler(recoverer, new FixedBackOff(1_000L, 3L));
        errorHandler.addNotRetryableExceptions(BusinessException.class);
        return errorHandler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> withdrawFeeKafkaListenerContainerFactory(
            ConsumerFactory<Object, Object> consumerFactory,
            DefaultErrorHandler withdrawFeeDeadLetterErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(withdrawFeeDeadLetterErrorHandler);
        return factory;
    }
}