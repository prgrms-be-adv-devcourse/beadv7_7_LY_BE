package site.coreservice.product.application;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.common.event.EventPublisher;
import site.coreservice.global.event.OrderCompletedEvent;

/**
 * 주문 완료 발행을 흉내 내는 local 전용 도구. 실경로 전체(경매 낙찰 → 주문 생성 → 구매자 확정)를
 * 타지 않고 시세 적재만 떼어 확인할 때 쓴다.
 * <p>
 * 시세 적재가 쓰는 값은 경매 id와 완료시각 둘뿐이고 나머지는 경매 조회로 채우므로, 주문 id·구매자·
 * 판매자·낙찰가는 자리만 채운다. 여기에 의미 있는 값을 넣으면 시세가 그 값을 쓴다고 오해하게 된다.
 * <p>
 * 트랜잭션 안에서 발행하는 이유: 수신 리스너가 "커밋 후"에만 반응하므로, 트랜잭션 없이 발행하면
 * 아무 일도 일어나지 않는다. 실제 발행 환경(주문 확정 트랜잭션 내부)과 같은 조건이기도 하다.
 * <p>
 * 주의: 이제 공용 이벤트를 발행하므로 예치금 쪽 리스너도 함께 깨운다. local 전용이고 실경로에서도
 * 같은 결합이 있으므로 그대로 둔다.
 */
@Profile("local")
@ConditionalOnProperty(name = "product.fake-trade.enabled", havingValue = "true")
@Service
@RequiredArgsConstructor
public class FakeTradePublishService {

    private static final Long UNUSED_ID = 0L;

    private final EventPublisher eventPublisher;

    @Transactional
    public void publishFakeTradeConfirmed(Long auctionId, LocalDateTime confirmedAt) {
        LocalDateTime effectiveConfirmedAt = (confirmedAt != null) ? confirmedAt : LocalDateTime.now();
        eventPublisher.publish(new OrderCompletedEvent(UNUSED_ID, auctionId, UNUSED_ID, UNUSED_ID,
                BigDecimal.ZERO, effectiveConfirmedAt));
    }
}
