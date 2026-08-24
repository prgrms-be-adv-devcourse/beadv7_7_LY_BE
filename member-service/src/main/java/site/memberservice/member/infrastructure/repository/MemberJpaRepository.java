package site.memberservice.member.infrastructure.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import site.memberservice.member.domain.Email;
import site.memberservice.member.domain.Member;

import java.util.Optional;

public interface MemberJpaRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(Email email);

    boolean existsByNickname(String nickName);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Member m WHERE m.phoneNumber.hash = :hash")
    boolean existsByPhoneNumberHash(@Param("hash") String hash);
}
