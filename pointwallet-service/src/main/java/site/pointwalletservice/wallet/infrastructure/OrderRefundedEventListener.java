package site.pointwalletservice.wallet.infrastructure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import site.common.event.contract.OrderRefundedEvent;
import site.pointwalletservice.wallet.application.OrderRefundedEventHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderRefundedEventListener {

    private final OrderRefundedEventHandler orderRefundedEventHandler;

    @KafkaListener(topics = "#{T(site.common.event.contract.EventType).ORDER_REFUNDED_EVENT.getValue()}",
            groupId = "pointwallet-service")
    public void handle(final OrderRefundedEvent event) {
        log.info("주문 환불 이벤트 수신: orderId={}, auctionId={}, buyerId={}",
                event.getOrderId(), event.getAuctionId(), event.getBuyerId());
        try {
            orderRefundedEventHandler.handle(event);
        } catch (DataIntegrityViolationException e) {
            // point_transaction(related_id, type) 유니크 제약 위반 — existsByRelatedIdAndType
            // 체크를 통과한 뒤에도 동시에 같은 이벤트가 처리돼서 경합이 실제로 발생한 경우다.
            // WithdrawFeeEarnedEventListener/SettlementConfirmedEventListener와 동일 패턴.
            log.warn("중복 전달로 인한 유니크 제약 위반 — 이미 처리된 이벤트로 간주하고 건너뜀. orderId={}",
                    event.getOrderId(), e);
        }
    }
}