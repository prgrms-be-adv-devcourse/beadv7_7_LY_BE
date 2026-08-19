package site.memberservice.member.infrastructure.event.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import site.common.event.contract.OrderCancelledEvent;
import site.memberservice.member.application.MemberService;
import site.memberservice.member.application.dto.RecordWinningBidOrderCancellationCommand;

@Slf4j
@RequiredArgsConstructor
@Component
public class MemberRestrictionEventListener {

    private final MemberService memberService;

    @KafkaListener(
        topics = "#{T(site.common.event.contract.EventType).ORDER_CANCELLED_EVENT.getValue()}",
        groupId = "member-service"
    )
    public void consumeWinningBidOrderCanceledEvent(final OrderCancelledEvent event) {
        try {
            final RecordWinningBidOrderCancellationCommand recordWinningBidOrderCancellationCommand = new RecordWinningBidOrderCancellationCommand(
                event.getBuyerId(),
                event.getOrderId(),
                event.getAuctionId(),
                event.getOccurredAt()
            );

            memberService.recordWinningBidOrderCancellation(recordWinningBidOrderCancellationCommand);
        } catch (final Exception e) {
            log.warn("낙찰된 주문 취소 이벤트 처리 과정에 문제가 발생하였습니다.", e);
        }
    }
}
