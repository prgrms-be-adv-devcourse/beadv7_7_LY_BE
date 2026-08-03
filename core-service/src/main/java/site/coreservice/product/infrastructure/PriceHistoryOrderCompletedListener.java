package site.coreservice.product.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import site.common.event.contract.OrderCompletedEvent;
import site.coreservice.product.application.PriceHistoryRecordService;
import site.coreservice.product.exception.AuctionContractViolationException;
import site.coreservice.product.exception.PriceHistoryAuctionNotClosedException;
import site.coreservice.product.exception.PriceHistoryAuctionNotFoundException;

/**
 * 주문 완료 이벤트의 수신 창구. 처리 내용은 전부 서비스에 있고 여기는 "어떻게 전달받는가"만 안다.
 * <p>
 * 클래스명이 이벤트명과 다른 이유: 같은 이벤트를 구독하는 리스너가 예치금 쪽에도 있어서, 단순
 * 클래스명을 맞추면 빈 이름이 겹쳐 애플리케이션이 기동되지 않는다.
 * <p>
 * 발행(OrderEventPublisher)이 아직 outbox 패턴 없이 트랜잭션 내부에서 바로 Kafka로 나가기 때문에,
 * 발행자 트랜잭션이 롤백돼도 이미 나간 메시지는 취소되지 않는다 — 즉 롤백된 주문완료에 대해서도
 * 시세가 적재될 수 있다는 알려진 한계다. outbox 도입 전까지는 이 리스너가 해결할 수 있는 문제가 아니다.
 * <p>
 * catch로 전부 삼키는 이유: 새어나간 예외는 컨테이너의 재시도를 태우게 되는데 아직 재시도 정책을
 * 따로 설계하지 않았다. 시세 적재 실패가 다른 리스너의 처리를 막으면 안 된다.
 * <p>
 * 삼키더라도 세 갈래로 나눠 남긴다. 갈래마다 사람이 할 일이 다르기 때문이다 — 보낸 쪽을 봐야 하는지,
 * 경매 API 스펙을 봐야 하는지, 그냥 다시 넣으면 되는지. 한 문구로 뭉치면 재시도해도 소용없는 실패를
 * 붙잡고 재시도하게 되거나, 정말 다시 넣으면 될 건이 그냥 버려진다.
 * <p>
 * 이 경로에는 로그 말고 다른 출구가 없다 — 컨슈머 스레드라 응답으로 나갈 곳도, 예외를 받아줄 핸들러도
 * 없다. 여기서 안 남기면 흔적 없이 사라진다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PriceHistoryOrderCompletedListener {

    private final PriceHistoryRecordService priceHistoryRecordService;

    @KafkaListener(topics = "#{T(site.common.event.contract.EventType).ORDER_COMPLETED_EVENT.getValue()}",
            groupId = "product-service")
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
