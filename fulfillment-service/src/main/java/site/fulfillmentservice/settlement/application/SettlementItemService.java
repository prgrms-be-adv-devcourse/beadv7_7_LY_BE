package site.fulfillmentservice.settlement.application;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.common.event.contract.OrderCompletedEvent;
import site.fulfillmentservice.settlement.domain.CommissionPolicy;
import site.fulfillmentservice.settlement.domain.CommissionPolicyRepository;
import site.fulfillmentservice.settlement.domain.Money;
import site.fulfillmentservice.settlement.domain.SettlementItem;
import site.fulfillmentservice.settlement.domain.SettlementItemRepository;
import site.fulfillmentservice.settlement.exception.SettlementErrorCode;
import site.fulfillmentservice.settlement.exception.SettlementException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SettlementItemService {

    private final SettlementItemRepository settlementItemRepository;
    private final CommissionPolicyRepository commissionPolicyRepository;

    public void createSettlementItem(OrderCompletedEvent event) {
        if (settlementItemRepository.existsByOrderId(event.getOrderId())) {
            log.info("이미 정산 항목이 생성된 주문입니다. 중복 처리로 건너뜁니다. orderId={}", event.getOrderId());
            return;
        }

        BigDecimal commissionRate = findEffectiveCommissionRate(event.getOrderedAt());

        SettlementItem settlementItem = SettlementItem.of(
            event.getOrderId(),
            event.getSellerId(),
            Money.of(event.getFinalBidPrice()),
            commissionRate,
            event.getCompletedAt()
        );

        try {
            settlementItemRepository.save(settlementItem);
        } catch (DataIntegrityViolationException e) {
            log.warn("이미 정산 항목이 생성된 주문입니다(동시성). 중복 처리로 건너뜁니다. orderId={}", event.getOrderId());
        }
    }

    private BigDecimal findEffectiveCommissionRate(LocalDateTime orderedAt) {
        return commissionPolicyRepository.findEffectiveAt(orderedAt)
            .map(CommissionPolicy::getCommissionRate)
            .orElseThrow(() -> new SettlementException(SettlementErrorCode.EFFECTIVE_COMMISSION_POLICY_NOT_FOUND));
    }
}
