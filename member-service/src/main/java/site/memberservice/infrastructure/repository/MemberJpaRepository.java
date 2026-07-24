package site.memberservice.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.memberservice.domain.Member;

public interface MemberJpaRepository extends JpaRepository<Member, Long> {
}
