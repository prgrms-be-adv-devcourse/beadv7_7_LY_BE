package site.coreservice.pointwallet.hold.infrastructure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import site.common.event.contract.OrderCancelledEvent;
import site.coreservice.pointwallet.hold.application.HoldService;
import site.coreservice.pointwallet.hold.exception.HoldErrorCode;
import site.coreservice.pointwallet.hold.exception.HoldException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCancelledEventListener {

    private final HoldService holdService;

    @KafkaListener(topics = "#{T(site.common.event.contract.EventType).ORDER_CANCELLED_EVENT.getValue()}",
            groupId = "pointwallet-service")
    public void handle(OrderCancelledEvent event) {
        holdService.release(event.getAuctionId());
    }
}