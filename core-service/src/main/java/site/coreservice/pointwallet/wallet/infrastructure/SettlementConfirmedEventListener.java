package site.coreservice.pointwallet.wallet.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import site.common.event.contract.SettlementConfirmedEvent;
import site.coreservice.pointwallet.wallet.application.SettlementConfirmedEventHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementConfirmedEventListener {

    private final SettlementConfirmedEventHandler settlementConfirmedEventHandler;

    @EventListener
    public void handle(final SettlementConfirmedEvent event) {
        log.info(
                "정산 확정 이벤트 수신: settlementBatchId={}, sellerId={}, amount={}",
                event.getSettlementBatchId(), event.getSellerId(), event.getTotalAmount()
        );
        settlementConfirmedEventHandler.handle(event);
    }
}