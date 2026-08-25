package site.pointwalletservice.wallet.deadletter.infrastructure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import site.pointwalletservice.wallet.deadletter.domain.WithdrawFeeDeadLetter;
import site.pointwalletservice.wallet.deadletter.domain.WithdrawFeeDeadLetterRepository;
import site.pointwalletservice.withdraw.domain.event.WithdrawFeeEarnedEvent;

/**
 * KafkaErrorHandlerConfig의 DeadLetterPublishingRecoverer가 재시도 소진 시 원본 레코드를 그대로
 * 재발행하는 "{topic}.DLT" 토픽을 구독해, WithdrawFeeEarnedEvent 실패 건만 골라 DB에 남긴다.
 * <p>
 * 다른 리스너(OrderRefundedEvent 등)의 실패도 각자의 .DLT 토픽에 격리는 되지만, 여기서는
 * withdraw 수수료 적립 건만 다룬다 - 이벤트마다 "관리자가 봐야 할 만큼 중요한지"가 달라서,
 * 필요한 이벤트에만 선택적으로 DLT 컨슈머 겸 관리자 조회 기능을 붙이는 정책이다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawFeeDeadLetterListener {

    private final WithdrawFeeDeadLetterRepository repository;

    @KafkaListener(
            id = "withdrawFeeDeadLetterListener",
            topics = WithdrawFeeEarnedEvent.TOPIC + ".DLT",
            groupId = "pointwallet-service-dlt"
    )
    public void handle(
            WithdrawFeeEarnedEvent event,
            @Header(value = KafkaHeaders.DLT_EXCEPTION_MESSAGE, required = false) String causeMessage
    ) {
        log.error("인출 수수료 이벤트 재시도 소진 - DLT로 격리됨: withdrawId={}, feeAmount={}, cause={}",
                event.withdrawId(), event.feeAmount(), causeMessage);
        repository.save(WithdrawFeeDeadLetter.open(event.withdrawId(), event.feeAmount(), causeMessage));
    }
}