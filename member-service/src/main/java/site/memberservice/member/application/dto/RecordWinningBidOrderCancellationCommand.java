package site.memberservice.member.application.dto;

import java.time.LocalDateTime;

public record RecordWinningBidOrderCancellationCommand(
    Long memberId,
    Long orderId,
    Long auctionId,
    LocalDateTime occurredAt
) {
}
