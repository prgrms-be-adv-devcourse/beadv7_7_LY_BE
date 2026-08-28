package site.memberservice.member.domain.repository;

import site.memberservice.member.domain.Member;

import java.util.Optional;

public interface MemberRepository {

    Member save(Member member);

    Optional<Member> findById(Long memberId);

    Optional<Member> findByEmailHash(String emailHash);

    boolean existsByNickname(String nickName);

    boolean existsByEmailHash(String emailHash);

    boolean existsByPhoneNumberHash(String phoneNumberHash);
}
