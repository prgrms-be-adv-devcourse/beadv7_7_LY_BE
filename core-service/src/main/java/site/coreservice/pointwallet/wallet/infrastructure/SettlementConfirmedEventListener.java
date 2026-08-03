package site.coreservice.pointwallet.wallet.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import site.common.event.contract.SettlementConfirmedEvent;
import site.coreservice.pointwallet.wallet.application.SettlementConfirmedEventHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementConfirmedEventListener {

    private final SettlementConfirmedEventHandler settlementConfirmedEventHandler;

    @KafkaListener(topics = "#{T(site.common.event.contract.EventType).SETTLEMENT_CONFIRMED_EVENT.getValue()}",
            groupId = "pointwallet-service")
    public void handle(final SettlementConfirmedEvent event) {
        log.info(
                "정산 확정 이벤트 수신: settlementBatchId={}, sellerId={}, amount={}",
                event.getSettlementBatchId(), event.getSellerId(), event.getTotalAmount()
        );
        settlementConfirmedEventHandler.handle(event);
    }
}