package site.coreservice.product.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import site.coreservice.product.application.PriceHistoryRecordService;
import site.coreservice.product.domain.TradeConfirmedEvent;
import site.coreservice.product.exception.PriceHistoryAuctionNotClosedException;
import site.coreservice.product.exception.PriceHistoryAuctionNotFoundException;

/**
 * 거래확정 이벤트의 수신 창구. 처리 내용은 전부 서비스에 있고 여기는 "어떻게 전달받는가"만 안다 —
 * 나중에 전달 수단이 바뀌면(예: 메시지 브로커) 이 클래스만 갈아끼운다.
 * <p>
 * AFTER_COMMIT: 발행자 트랜잭션이 커밋된 뒤에만 반응한다. 커밋 전에 반응하면 발행자가 롤백됐는데
 * 시세만 남는 유령 데이터가 생긴다.
 * <p>
 * catch로 전부 삼키는 이유: 커밋 후 콜백에서 새어나간 예외는 (이미 커밋에 성공한) 발행자에게 전파되고,
 * 같은 커밋을 기다리던 다른 수신자들의 실행까지 끊는다. 시세 적재 실패가 남의 흐름을 깨면 안 된다.
 * <p>
 * 삼키더라도 두 갈래로 나눠 남긴다. 경매를 못 찾거나 마감 상태가 아닌 경우는 정상 흐름에서는 나올 수 없는
 * 값이 실려왔다는 뜻이라 보낸 쪽을 사람이 확인해야 하고, 그 외(DB 일시 장애 등)는 다시 시도하면 되는
 * 실패다. 한 문구로 뭉쳐 찍으면 로그만 보고 둘을 구분할 수 없어 대응 시점을 놓친다.
 * <p>
 * 이 경로에는 로그 말고 다른 출구가 없다 — 요청 처리 중이 아니라 커밋 후 콜백이라 응답으로 나갈 곳도,
 * 예외를 받아줄 핸들러도 없다. 여기서 안 남기면 흔적 없이 사라진다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TradeConfirmedEventListener {

    private final PriceHistoryRecordService priceHistoryRecordService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTradeConfirmedEvent(TradeConfirmedEvent event) {
        try {
            priceHistoryRecordService.recordConfirmedTrade(event.getAuctionId(), event.getConfirmedAt());
        } catch (PriceHistoryAuctionNotFoundException | PriceHistoryAuctionNotClosedException e) {
            log.error("[시세적재-데이터불일치] {} — {}", e.getErrorCode().getValue(), e.getMessage(), e);
        } catch (Exception e) {
            log.error("거래확정 이벤트 처리 실패 — auctionId: {}", event.getAuctionId(), e);
        }
    }
}
