package site.pointwalletservice.wallet.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.common.event.EventHandler;
import site.common.event.contract.SettlementConfirmedEvent;
import site.pointwalletservice.ledger.application.PointTransactionService;
import site.pointwalletservice.ledger.domain.PointTransactionType;
import site.pointwalletservice.shared.Money;


/**
 * 정산(settlement) 도메인이 배치를 확정하면 SettlementConfirmedEvent를 발행하는데,
 * 지금까지 예치금 쪽에 이걸 받는 리스너가 없어 판매자 지갑에 정산금이 실제로 입금되지 않고 있었다.
 * 이 핸들러가 이벤트를 받아 판매자 지갑에 입금 + 원장(ledger)에 기록하는 역할을 한다.
 * <p>
 * charge()를 쓴다 - credit()과 달리 지갑이 없으면 자동으로 개설한다.
 * 판매자는 한 번도 충전/입찰을 안 해봤을 수도 있어서(구매 이력 없이 판매만 하는 유저),
 * "지갑이 없는 게 비정상 데이터"인 releasePreviousHold 케이스와는 다르다.
 * <p>
 * 카프카는 at-least-once 전달이라 같은 이벤트가 재전달될 수 있다 - 재전달 시 charge()를 다시
 * 태우면 판매자한테 정산금이 이중 지급된다. WithdrawFeeEarnedEventHandler와 동일한 패턴으로
 * existsForRelatedId(settlementBatchId, SETTLEMENT_PAYOUT)로 이미 처리한 배치인지
 * 먼저 확인하고, 이미 처리됐으면 조용히 스킵한다.
 * <p>
 * ledger 도메인엔 PointTransactionService(포트)로만 접근한다 - PointTransactionRepository를
 * 직접 주입받지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementConfirmedEventHandler implements EventHandler<SettlementConfirmedEvent> {

    private final WalletService walletService;
    private final PointTransactionService pointTransactionService;

    @Override
    @Transactional
    public void handle(final SettlementConfirmedEvent event) {
        if (pointTransactionService.existsForRelatedId(
                event.getSettlementBatchId(), PointTransactionType.SETTLEMENT_PAYOUT)) {
            log.info("이미 입금 처리된 정산 배치입니다. 중복 처리로 건너뜁니다. settlementBatchId={}",
                    event.getSettlementBatchId());
            return;
        }

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