package site.memberservice.member.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.memberservice.member.domain.Member;
import site.memberservice.member.domain.repository.MemberAddressView;
import site.memberservice.member.domain.repository.MemberCredentials;
import site.memberservice.member.domain.repository.MemberProfileView;
import site.memberservice.member.domain.repository.MemberRepository;

import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class MemberRepositoryImpl implements MemberRepository {

    private final MemberJpaRepository memberJpaRepository;

    @Override
    public Member save(final Member member) {
        return memberJpaRepository.save(member);
    }

    @Override
    public Optional<Member> findById(final Long memberId) {
        return memberJpaRepository.findById(memberId);
    }

    @Override
    public Optional<Member> findByEmailHash(final String emailHash) {
        return memberJpaRepository.findByEmailHash(emailHash);
    }

    @Override
    public boolean existsById(final Long memberId) {
        return memberJpaRepository.existsById(memberId);
    }

    @Override
    public boolean existsByNickname(final String nickName) {
        return memberJpaRepository.existsByNickname(nickName);
    }

    @Override
    public boolean existsByEmailHash(final String emailHash) {
        return memberJpaRepository.existsByEmailHash(emailHash);
    }

    @Override
    public boolean existsByPhoneNumberHash(final String phoneNumberHash) {
        return memberJpaRepository.existsByPhoneNumberHash(phoneNumberHash);
    }

    @Override
    public Optional<MemberCredentials> findCredentialsByEmailHash(final String emailHash) {
        return memberJpaRepository.findCredentialsByEmailHash(emailHash);
    }

    @Override
    public Optional<String> findNicknameById(final Long memberId) {
        return memberJpaRepository.findNicknameById(memberId);
    }

    @Override
    public Optional<MemberProfileView> findProfileById(final Long memberId) {
        return memberJpaRepository.findProfileById(memberId);
    }

    @Override
    public Optional<MemberAddressView> findAddressViewById(final Long memberId) {
        return memberJpaRepository.findAddressViewById(memberId);
    }

    @Override
    public Optional<String> findNameById(final Long memberId) {
        return memberJpaRepository.findNameById(memberId);
    }

    @Override
    public Member getReferenceById(final Long memberId) {
        return memberJpaRepository.getReferenceById(memberId);
    }
}
