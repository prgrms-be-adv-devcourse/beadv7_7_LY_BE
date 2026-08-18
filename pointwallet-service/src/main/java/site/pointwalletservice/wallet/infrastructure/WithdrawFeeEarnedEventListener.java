package site.pointwalletservice.wallet.infrastructure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import site.pointwalletservice.wallet.application.WithdrawFeeEarnedEventHandler;
import site.pointwalletservice.withdraw.domain.event.WithdrawFeeEarnedEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawFeeEarnedEventListener {

    private final WithdrawFeeEarnedEventHandler withdrawFeeEarnedEventHandler;

    @KafkaListener(id = "withdrawFeeEarnedEventListener", topics = WithdrawFeeEarnedEvent.TOPIC, groupId = "pointwallet-service")
    public void handle(WithdrawFeeEarnedEvent event) {
        log.info("인출 수수료 이벤트 수신: withdrawId={}, feeAmount={}", event.withdrawId(), event.feeAmount());
        try {
            withdrawFeeEarnedEventHandler.handle(event);
        } catch (DataIntegrityViolationException e) {
            // point_transaction(related_id, type) 유니크 제약 위반 — existsByRelatedIdAndType
            // 체크를 통과한 뒤에도 동시에 같은 이벤트가 처리돼서 경합이 실제로 발생한 경우다.
            // 트랜잭션은 이미 롤백됐으니(charge()까지 포함) 여기서 잡아 정상 종료시키는 것으로
            // 중복 처리를 안전하게 마무리한다. 리스너 밖으로 던지면 Kafka가 재시도/DLQ로
            // 취급해버리므로, 여기가 이 예외를 삼켜야 할 위치다.
            log.warn("중복 전달로 인한 유니크 제약 위반 — 이미 처리된 이벤트로 간주하고 건너뜀. withdrawId={}",
                    event.withdrawId(), e);
        }
    }
}