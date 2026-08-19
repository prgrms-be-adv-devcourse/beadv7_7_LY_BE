package site.memberservice.member.domain.repository;

import site.memberservice.member.domain.Member;
import site.memberservice.member.domain.MemberViolationHistory;
import site.memberservice.member.domain.ViolationType;

import java.time.LocalDateTime;

public interface MemberViolationHistoryRepository {

    MemberViolationHistory save(MemberViolationHistory memberViolationHistory);

    long countByMemberAndViolationTypeSince(Member member, ViolationType violationType, LocalDateTime since);

    boolean hasWinningBidOrderCancellationRecord(Member member, ViolationType violationType, Long orderId);
}
