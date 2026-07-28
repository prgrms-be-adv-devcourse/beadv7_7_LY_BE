package site.memberservice.member.domain.repository;

import site.memberservice.member.domain.Email;
import site.memberservice.member.domain.Member;
import site.memberservice.member.domain.PhoneNumber;

import java.util.Optional;

public interface MemberRepository {

    Member save(Member member);

    Optional<Member> findById(Long memberId);

    Optional<Member> findByEmail(Email email);

    boolean existsByNickname(String nickName);

    boolean existsByPhoneNumber(PhoneNumber phoneNumber);
}
