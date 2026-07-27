package site.coreservice.pointwallet.hold.infrastructure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import site.coreservice.global.event.OrderCompletedEvent;
import site.coreservice.pointwallet.hold.application.HoldService;
import site.coreservice.pointwallet.hold.exception.HoldErrorCode;
import site.coreservice.pointwallet.hold.exception.HoldException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCompletedEventListener {

    private final HoldService holdService;

    @EventListener
    public void handle(OrderCompletedEvent event) {
        try {
            holdService.consume(event.getAuctionId());
        } catch (HoldException e) {
            if (e.getErrorCode() == HoldErrorCode.HOLD_NOT_FOUND) {
                log.warn("OrderCompletedEvent 처리 중 소멸시킬 홀드 없음, 스킵: auctionId={}", event.getAuctionId());
                return;
            }
            throw e;
        }
    }
}