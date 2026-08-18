package site.memberservice.member.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query(value = """
        SELECT COUNT(*)
        FROM member_violation_history 
        WHERE member_id = :memberId 
          AND violation_type = :violationType 
          AND JSON_EXTRACT(details, '$.orderId') = :orderId
    """, nativeQuery = true)
    long countByMemberIdAndViolationTypeAndOrderId(
        @Param("memberId") Long memberId,
        @Param("violationType") String violationType,
        @Param("orderId") Long orderId
    );
}
