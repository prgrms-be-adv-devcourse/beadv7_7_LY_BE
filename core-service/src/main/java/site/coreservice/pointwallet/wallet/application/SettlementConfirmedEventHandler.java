package site.coreservice.pointwallet.wallet.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.common.event.EventHandler;
import site.coreservice.global.event.SettlementConfirmedEvent;
import site.coreservice.pointwallet.ledger.application.PointTransactionService;
import site.coreservice.pointwallet.ledger.domain.PointTransactionType;
import site.coreservice.pointwallet.shared.Money;

/**
 * 정산(settlement) 도메인이 배치를 확정하면 SettlementConfirmedEvent를 발행하는데,
 * 지금까지 예치금 쪽에 이걸 받는 리스너가 없어 판매자 지갑에 정산금이 실제로 입금되지 않고 있었다.
 * 이 핸들러가 이벤트를 받아 판매자 지갑에 입금 + 원장(ledger)에 기록하는 역할을 한다.
 * <p>
 * charge()를 쓴다 - credit()과 달리 지갑이 없으면 자동으로 개설한다.
 * 판매자는 한 번도 충전/입찰을 안 해봤을 수도 있어서(구매 이력 없이 판매만 하는 유저),
 * "지갑이 없는 게 비정상 데이터"인 releasePreviousHold 케이스와는 다르다.
 */
@Component
@RequiredArgsConstructor
public class SettlementConfirmedEventHandler implements EventHandler<SettlementConfirmedEvent> {

    private final WalletService walletService;
    private final PointTransactionService pointTransactionService;

    @Override
    @Transactional
    public void handle(final SettlementConfirmedEvent event) {
        Money amount = Money.of(event.getTotalAmount());

        WalletBalanceResult result = walletService.charge(event.getSellerId(), amount);

        pointTransactionService.record(
                result.walletId(),
                PointTransactionType.SETTLEMENT_PAYOUT,
                amount,
                result.balanceAfter(),
                event.getSettlementBatchId()
        );
    }
}