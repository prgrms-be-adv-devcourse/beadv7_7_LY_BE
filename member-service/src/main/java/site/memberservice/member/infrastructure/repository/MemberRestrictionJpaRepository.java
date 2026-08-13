package site.memberservice.member.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.memberservice.member.domain.Member;
import site.memberservice.member.domain.MemberRestriction;

import java.time.LocalDateTime;
import java.util.List;

public interface MemberRestrictionJpaRepository extends JpaRepository<MemberRestriction, Long> {

    List<MemberRestriction> findByMemberAndRestrictedAtLessThanEqualAndRestrictedUntilAfter(
        Member member,
        LocalDateTime restrictedAt,
        LocalDateTime restrictedUntil
    );
}
