package site.memberservice.member.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.memberservice.member.domain.Member;
import site.memberservice.member.domain.MemberRestriction;
import site.memberservice.member.domain.repository.MemberRestrictionRepository;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Repository
public class MemberRestrictionRepositoryImpl implements MemberRestrictionRepository {

    private final MemberRestrictionJpaRepository memberRestrictionJpaRepository;

    @Override
    public MemberRestriction save(final MemberRestriction memberRestriction) {
        return memberRestrictionJpaRepository.save(memberRestriction);
    }

    @Override
    public List<MemberRestriction> findActiveByMember(final Member member, final LocalDateTime now) {
        return memberRestrictionJpaRepository.findByMemberAndRestrictedAtLessThanEqualAndRestrictedUntilAfter(member, now, now);
    }
}
