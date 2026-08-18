package site.memberservice.member.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.memberservice.member.domain.Member;
import site.memberservice.member.domain.MemberViolationHistory;
import site.memberservice.member.domain.ViolationType;
import site.memberservice.member.domain.repository.MemberViolationHistoryRepository;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Repository
public class MemberViolationHistoryRepositoryImpl implements MemberViolationHistoryRepository {

    private final MemberViolationHistoryJpaRepository memberViolationHistoryJpaRepository;

    @Override
    public MemberViolationHistory save(final MemberViolationHistory memberViolationHistory) {
        return memberViolationHistoryJpaRepository.save(memberViolationHistory);
    }

    @Override
    public long countByMemberAndViolationTypeSince(final Member member, final ViolationType violationType, final LocalDateTime since) {
        return memberViolationHistoryJpaRepository.countByMemberAndViolationTypeAndOccurredAtAfter(member, violationType, since);
    }
}
