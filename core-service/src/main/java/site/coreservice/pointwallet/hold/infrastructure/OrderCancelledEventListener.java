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
        try {
            holdService.release(event.getAuctionId());
        } catch (HoldException e) {
            if (e.getErrorCode() == HoldErrorCode.HOLD_NOT_FOUND) {
                // 이미 처리됐거나(중복 이벤트) 순서가 꼬여 홀드가 없는 경우 - 스킵
                log.warn("OrderCancelledEvent 처리 중 해제할 홀드 없음, 스킵: auctionId={}", event.getAuctionId());
                return;
            }
            throw e;
        }
    }
}