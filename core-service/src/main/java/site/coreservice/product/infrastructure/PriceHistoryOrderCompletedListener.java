package site.coreservice.product.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import site.coreservice.global.event.OrderCompletedEvent;
import site.coreservice.product.application.PriceHistoryRecordService;
import site.coreservice.product.exception.AuctionContractViolationException;
import site.coreservice.product.exception.PriceHistoryAuctionNotClosedException;
import site.coreservice.product.exception.PriceHistoryAuctionNotFoundException;

/**
 * 주문 완료 이벤트의 수신 창구. 처리 내용은 전부 서비스에 있고 여기는 "어떻게 전달받는가"만 안다 —
 * 나중에 전달 수단이 바뀌면(예: 메시지 브로커) 이 클래스만 갈아끼운다.
 * <p>
 * 클래스명이 이벤트명과 다른 이유: 같은 이벤트를 구독하는 리스너가 예치금 쪽에도 있어서, 단순
 * 클래스명을 맞추면 빈 이름이 겹쳐 애플리케이션이 기동되지 않는다.
 * <p>
 * AFTER_COMMIT: 발행자 트랜잭션이 커밋된 뒤에만 반응한다. 커밋 전에 반응하면 발행자가 롤백됐는데
 * 시세만 남는 유령 데이터가 생긴다.
 * <p>
 * catch로 전부 삼키는 이유: 커밋 후 콜백에서 새어나간 예외는 (이미 커밋에 성공한) 발행자에게 전파되고,
 * 같은 커밋을 기다리던 다른 수신자들의 실행까지 끊는다. 시세 적재 실패가 남의 흐름을 깨면 안 된다.
 * <p>
 * 삼키더라도 세 갈래로 나눠 남긴다. 갈래마다 사람이 할 일이 다르기 때문이다 — 보낸 쪽을 봐야 하는지,
 * 경매 API 스펙을 봐야 하는지, 그냥 다시 넣으면 되는지. 한 문구로 뭉치면 재시도해도 소용없는 실패를
 * 붙잡고 재시도하게 되거나, 정말 다시 넣으면 될 건이 그냥 버려진다.
 * <p>
 * 이 경로에는 로그 말고 다른 출구가 없다 — 요청 처리 중이 아니라 커밋 후 콜백이라 응답으로 나갈 곳도,
 * 예외를 받아줄 핸들러도 없다. 여기서 안 남기면 흔적 없이 사라진다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PriceHistoryOrderCompletedListener {

    private final PriceHistoryRecordService priceHistoryRecordService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCompletedEvent(OrderCompletedEvent event) {
        if (event.getAuctionId() == null || event.getCompletedAt() == null) {
            log.error("[시세적재-이벤트결함] 필수값이 비어 적재를 건너뜁니다 — orderId: {}, auctionId: {}, completedAt: {}",
                    event.getOrderId(), event.getAuctionId(), event.getCompletedAt());
            return;
        }
        try {
            priceHistoryRecordService.recordConfirmedTrade(event.getAuctionId(), event.getCompletedAt());
        } catch (PriceHistoryAuctionNotFoundException | PriceHistoryAuctionNotClosedException e) {
            log.error("[시세적재-데이터불일치] {} — {} (다시 넣어도 같은 결과, 보낸 쪽 확인 필요)",
                    e.getErrorCode().getValue(), e.getMessage(), e);
        } catch (AuctionContractViolationException e) {
            log.error("[시세적재-계약위반] {} — {} (다시 넣어도 같은 결과, 경매 API 응답 확인 필요)",
                    e.getErrorCode().getValue(), e.getMessage(), e);
        } catch (Exception e) {
            log.error("[시세적재-실패] auctionId: {} — 잠시 뒤 다시 넣으면 되는 실패, 수동 재적재 필요",
                    event.getAuctionId(), e);
        }
    }
}
