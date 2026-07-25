package site.coreservice.product.application;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.common.event.EventPublisher;
import site.coreservice.product.domain.TradeConfirmedEvent;

/**
 * 주문(06)의 거래확정 발행을 흉내 내는 local 전용 도구. 실제 발행자가 준비되면 삭제 대상.
 * 트랜잭션 안에서 발행하는 이유: 수신 리스너가 "커밋 후"에만 반응하므로, 트랜잭션 없이 발행하면
 * 아무 일도 일어나지 않는다. 실제 발행 환경(주문 확정 트랜잭션 내부)과 같은 조건이기도 하다.
 */
@Profile("local")
@ConditionalOnProperty(name = "product.fake-trade.enabled", havingValue = "true")
@Service
@RequiredArgsConstructor
public class FakeTradePublishService {

    private final EventPublisher eventPublisher;

    @Transactional
    public void publishFakeTradeConfirmed(Long auctionId, LocalDateTime confirmedAt) {
        LocalDateTime effectiveConfirmedAt = (confirmedAt != null) ? confirmedAt : LocalDateTime.now();
        eventPublisher.publish(new TradeConfirmedEvent(auctionId, effectiveConfirmedAt));
    }
}
