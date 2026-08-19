package site.pointwalletservice.hold.infrastructure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import site.common.event.contract.AuctionForceCanceledEvent;
import site.pointwalletservice.hold.application.HoldService;

/**
 * 경매를 관리자가 강제 종료하면 auction-service가 이 이벤트를 발행해 입찰자의 홀드를 풀어달라고
 * 요청한다(#238). 지금까지 예치금 쪽에 이걸 받는 리스너가 없어 강제종료된 경매의 입찰자 돈이
 * 홀드된 채로 풀리지 않고 있었다.
 * <p>
 * OrderCancelledEventListener와 동일한 패턴 — release()는 대상 Hold가 없으면 예외 없이 스킵하는
 * 멱등한 구조라, KafkaErrorHandlerConfig의 재시도 정책을 그대로 적용해도 안전하다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionForceCanceledEventListener {

    private final HoldService holdService;

    @KafkaListener(topics = "#{T(site.common.event.contract.EventType).AUCTION_FORCE_CANCELED_EVENT.getValue()}",
            groupId = "pointwallet-service")
    public void handle(AuctionForceCanceledEvent event) {
        holdService.release(event.getAuctionId());
    }
}