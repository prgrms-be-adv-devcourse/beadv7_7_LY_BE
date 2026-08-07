package site.fulfillmentservice.order.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCompleteScheduler {

    private static final long POLL_INTERVAL_MILLIS = 60_000L;

    private final OrderService orderService;

    @Scheduled(fixedDelay = POLL_INTERVAL_MILLIS)
    public void completeExpiredOrders() {
        List<Long> orderIdsToComplete = orderService.findOrdersToAutoComplete();

        for (Long orderId : orderIdsToComplete) {
            try {
                orderService.completeExpiredOrder(orderId);
            } catch (Exception e) {
                log.error("주문 자동 완료 처리 실패: orderId={}", orderId, e);
            }
        }
    }
}
