package site.pointwalletservice.wallet.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.common.event.EventHandler;
import site.common.event.contract.OrderRefundedEvent;
import site.pointwalletservice.ledger.application.PointTransactionService;
import site.pointwalletservice.ledger.domain.PointTransactionType;
import site.pointwalletservice.ledger.exception.LedgerErrorCode;
import site.pointwalletservice.ledger.exception.LedgerException;
import site.pointwalletservice.shared.Money;

/**
 * 주문 환불이 승인되면 fulfillment가 이 이벤트를 발행하는데, 지금까지 예치금 쪽에 이걸 받는
 * 리스너가 없어 환불 승인이 나도 구매자 지갑에 아무 반영이 안 되고 있었다.
 * <p>
 * OrderRefundedEvent엔 환불 금액이 없다(orderId/auctionId/buyerId뿐) - 낙찰금은 원장
 * (point_transaction)에서 PointTransactionService.findLatestHoldAmountByAuctionId()로 되짚는다.
 * Hold 로우 자체는 주문완료 시점(consume())에 이미 삭제됐지만, 원장은 auctionId를 따로
 * 들고 있어서(PointTransaction.auctionId) Hold가 사라진 뒤에도 조회 가능하다.
 * <p>
 * ledger 도메인엔 PointTransactionService(포트)로만 접근한다 - PointTransactionRepository를
 * 직접 주입받지 않는다. wallet 도메인이 ledger의 내부 저장 구조(엔티티 모양 등)를 알 필요가
 * 없어야 헥사고날 경계가 지켜진다.
 * <p>
 * charge()가 아니라 credit()을 쓴다 - 환불 대상은 이미 낙찰까지 받은 구매자라 지갑이 없는 게
 * 오히려 데이터 정합성 문제다. 조용히 새로 만들면 안 된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderRefundedEventHandler implements EventHandler<OrderRefundedEvent> {

    private final WalletService walletService;
    private final PointTransactionService pointTransactionService;

    @Override
    @Transactional
    public void handle(final OrderRefundedEvent event) {
        if (pointTransactionService.existsForRelatedId(event.getOrderId(), PointTransactionType.REFUND)) {
            log.info("이미 환불 처리된 주문입니다. 중복 처리로 건너뜁니다. orderId={}", event.getOrderId());
            return;
        }

        Money refundAmount = pointTransactionService.findLatestHoldAmountByAuctionId(event.getAuctionId())
                .orElseThrow(() -> new LedgerException(LedgerErrorCode.AUCTION_HOLD_LEDGER_NOT_FOUND));

        WalletBalanceResult result = walletService.credit(event.getBuyerId(), refundAmount);

        pointTransactionService.record(
                result.walletId(), PointTransactionType.REFUND,
                refundAmount, result.balanceAfter(), event.getOrderId()
        );
    }
}