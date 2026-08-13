package site.memberservice.member.infrastructure.event.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import site.common.event.contract.MemberBidRestrictedEvent;
import site.memberservice.member.application.MemberService;
import site.memberservice.member.application.dto.RestrictMemberCommand;
import site.memberservice.member.domain.RestrictionType;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
@Component
public class MemberRestrictionEventConsumer {

    private final MemberService memberService;

    @KafkaListener(
        topics = "#{T(site.common.event.contract.EventType).MEMBER_BID_RESTRICTED_EVENT.getValue()}",
        groupId = "member-service"
    )
    public void consumeMemberBidRestrictedEvent(final MemberBidRestrictedEvent event) {
        try {
            final RestrictMemberCommand restrictMemberCommand = new RestrictMemberCommand(
                event.getMemberId(),
                RestrictionType.AUCTION_BIDDING,
                "최근 30일간 낙찰 상품 주문 취소 3회 이상 누적",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(7)
            );

            memberService.restrictMember(restrictMemberCommand);
        } catch (final Exception e) {
            log.warn("회원 입찰 제재 로직에 문제가 발생하였습니다.", e);
        }
    }
}
