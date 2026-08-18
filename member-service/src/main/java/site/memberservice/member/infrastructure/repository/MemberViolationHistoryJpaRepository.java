package site.memberservice.member.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.memberservice.member.domain.Member;
import site.memberservice.member.domain.MemberViolationHistory;
import site.memberservice.member.domain.ViolationType;

import java.time.LocalDateTime;

public interface MemberViolationHistoryJpaRepository extends JpaRepository<MemberViolationHistory, Long> {

    long countByMemberAndViolationTypeAndOccurredAtAfter(
        Member member,
        ViolationType violationType,
        LocalDateTime occurredAt
    );
}
