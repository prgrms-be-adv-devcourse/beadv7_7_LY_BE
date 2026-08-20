package site.pointwalletservice.hold.infrastructure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import site.common.event.contract.OrderRefundedEvent;
import site.pointwalletservice.hold.application.HoldService;

/**
 * 주문 환불이 승인되면 fulfillment의 OrderService.approveRefund()가 이 이벤트를 발행한다.
 * <p>
 * fulfillment Order의 상태 가드를 보면(requestRefund()는 status==ORDERED만 허용,
 * approveRefund()는 status==REFUND_REQUESTED만 허용) 환불 흐름은
 * ORDERED → REFUND_REQUESTED → REFUND로만 진행되고 COMPLETED를 거치는 경로가 없다.
 * 즉 이 이벤트가 발행되는 시점엔 항상 Hold 로우가 아직 살아있다(consume()으로 삭제된 적 없음) —
 * OrderCancelledEvent/AuctionForceCanceledEvent와 정확히 같은 상황이라 같은 방식(release)으로
 * 처리한다.
 * <p>
 * (이전 구현은 Hold가 이미 consume()으로 사라졌다고 잘못 가정하고 원장(point_transaction)에서
 * auctionId로 홀드 금액을 되짚어 credit()하는 방식이었는데, 실제로는 Hold가 항상 살아있어서
 * 매 환불마다 Hold 로우가 안 지워진 채 남고 지갑에 중복으로 돈이 들어갈 수 있는 버그였다.)
 * <p>
 * release()는 대상 Hold가 없으면 예외 없이 스킵하는 멱등한 구조라, KafkaErrorHandlerConfig의
 * 재시도 정책을 그대로 적용해도 안전하다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderRefundedEventListener {

    private final HoldService holdService;

    @KafkaListener(topics = "#{T(site.common.event.contract.EventType).ORDER_REFUNDED_EVENT.getValue()}",
            groupId = "pointwallet-service")
    public void handle(OrderRefundedEvent event) {
        holdService.release(event.getAuctionId());
    }
}