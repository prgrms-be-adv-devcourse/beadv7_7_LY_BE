// wallet/infrastructure/WithdrawFeeEarnedEventListener.java
package site.pointwalletservice.wallet.infrastructure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import site.pointwalletservice.wallet.application.WithdrawFeeEarnedEventHandler;
import site.pointwalletservice.withdraw.domain.event.WithdrawFeeEarnedEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawFeeEarnedEventListener {

    private final WithdrawFeeEarnedEventHandler withdrawFeeEarnedEventHandler;

    @KafkaListener(topics = WithdrawFeeEarnedEvent.TOPIC, groupId = "pointwallet-service")
    public void handle(WithdrawFeeEarnedEvent event) {
        log.info("인출 수수료 이벤트 수신: withdrawId={}, feeAmount={}", event.withdrawId(), event.feeAmount());
        withdrawFeeEarnedEventHandler.handle(event);
    }
}