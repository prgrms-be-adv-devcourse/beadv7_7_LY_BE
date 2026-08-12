package site.productservice.application.wishlist;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import site.common.event.EventPublisher;
import site.common.event.contract.WishlistChangedEvent;

/**
 * 위시리스트 변경 신호를 발행한다. 추가와 삭제가 같은 이벤트를 발행하지만, 발행에 실패했을 때 무엇 때문이었는지 로그로 남기려고 메서드를 나눠 뒀다.
 * <p>
 * 다른 EventPublisher들과 달리 <b>발행 실패를 삼킨다.</b> 이 이벤트는 트랜잭션이 커밋된 뒤에 발행되므로 (WishlistServiceFacade 참고),
 * 여기서 예외를 올리면 위시리스트에는 이미 반영됐는데 사용자만 실패 응답을 받는다. 재시도하면 유니크 제약에 걸려 또 실패한다.
 * <p>
 * 삼켜도 되는 이유는 이 이벤트가 반드시 발행이 보장되어야 하는 종류가 아니기 때문이다 — 유실되면 그 회원의 추천이 잠시 낡을 뿐이고, 추천 조회 시점에 현재 위시리스트와
 * 비교해 불일치를 감지하면 그 자리에서 복구된다. 정산·주문 이벤트였다면 이런 선택을 할 수 없다.
 * <p>
 * 대신 <b>이 로그가 유일한 추적 수단</b>이다. 이벤트 페이로드에는 memberId만 담기므로(델타로 오용되는 걸 막으려고 productId를 뺐다), 무엇 때문에 발행이
 * 시도됐는지는 여기서만 남는다.
 * <p>
 * 자세한 배경은 docs/wishlist-changed-event-design.md 참고.
 */
@Slf4j
@Component
public class WishlistEventPublisher {

    private final EventPublisher eventPublisher;

    public WishlistEventPublisher(final EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishAdded(final Long memberId, final Long productId) {
        publishChanged(memberId, productId);
    }

    public void publishRemoved(final Long memberId, final Long productId) {
        publishChanged(memberId, productId);
    }

    private void publishChanged(final Long memberId, final Long productId) {
        try {
            eventPublisher.publish(
                WishlistChangedEvent.builder()
                    .memberId(memberId)
                    .build());
        } catch (final Exception e) {
            log.error("WishlistChangedEvent 발행 실패 — memberId: {}, productId: {}. ", memberId,
                productId, e);
        }
    }
}
