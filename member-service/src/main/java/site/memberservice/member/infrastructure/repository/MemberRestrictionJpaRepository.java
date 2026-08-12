package site.memberservice.member.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.memberservice.member.domain.MemberRestriction;

public interface MemberRestrictionJpaRepository extends JpaRepository<MemberRestriction, Long> {
}
