package site.memberservice.member.domain.repository;

import site.memberservice.member.domain.Member;

import java.util.Optional;

public interface MemberRepository {

    Member save(Member member);

    Optional<Member> findById(Long memberId);

    Optional<Member> findByEmailHash(String emailHash);

    boolean existsById(Long memberId);

    boolean existsByNickname(String nickName);

    boolean existsByEmailHash(String emailHash);

    boolean existsByPhoneNumberHash(String phoneNumberHash);

    Optional<MemberCredentials> findCredentialsByEmailHash(String emailHash);

    Optional<String> findNicknameById(Long memberId);

    Optional<MemberProfileView> findProfileById(Long memberId);

    Optional<MemberAddressView> findAddressViewById(Long memberId);

    Optional<String> findNameById(Long memberId);

    Member getReferenceById(Long memberId);
}
