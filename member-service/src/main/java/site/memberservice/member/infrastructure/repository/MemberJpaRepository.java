package site.memberservice.member.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.memberservice.member.domain.Member;
import site.memberservice.member.domain.PhoneNumber;

public interface MemberJpaRepository extends JpaRepository<Member, Long> {

    boolean existsByNickname(String nickName);

    boolean existsByPhoneNumber(PhoneNumber phoneNumber);
}
