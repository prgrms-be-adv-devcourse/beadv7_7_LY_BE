package site.coreservice.pointwallet.hold.infrastructure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import site.coreservice.global.event.OrderCancelledEvent;
import site.coreservice.pointwallet.hold.application.HoldService;
import site.coreservice.pointwallet.hold.exception.HoldErrorCode;
import site.coreservice.pointwallet.hold.exception.HoldException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCancelledEventListener {

    private final HoldService holdService;

    @EventListener
    public void handle(OrderCancelledEvent event) {
        holdService.release(event.getAuctionId());
    }
}