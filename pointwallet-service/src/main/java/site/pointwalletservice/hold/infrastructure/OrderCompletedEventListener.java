package site.pointwalletservice.hold.infrastructure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import site.common.event.contract.OrderCompletedEvent;
import site.pointwalletservice.hold.application.HoldService;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCompletedEventListener {

    private final HoldService holdService;

    @KafkaListener(topics = "#{T(site.common.event.contract.EventType).ORDER_COMPLETED_EVENT.getValue()}",
            groupId = "pointwallet-service")
    public void handle(OrderCompletedEvent event) {
        holdService.consume(event.getAuctionId());
    }
}