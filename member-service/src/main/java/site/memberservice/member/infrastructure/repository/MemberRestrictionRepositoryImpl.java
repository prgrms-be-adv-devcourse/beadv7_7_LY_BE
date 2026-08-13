package site.memberservice.member.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.memberservice.member.domain.MemberRestriction;
import site.memberservice.member.domain.repository.MemberRestrictionRepository;

@RequiredArgsConstructor
@Repository
public class MemberRestrictionRepositoryImpl implements MemberRestrictionRepository {

    private final MemberRestrictionJpaRepository memberRestrictionJpaRepository;

    @Override
    public MemberRestriction save(final MemberRestriction memberRestriction) {
        return memberRestrictionJpaRepository.save(memberRestriction);
    }
}
