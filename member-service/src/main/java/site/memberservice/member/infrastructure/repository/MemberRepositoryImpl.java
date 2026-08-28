package site.memberservice.member.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.memberservice.member.domain.Member;
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
}
