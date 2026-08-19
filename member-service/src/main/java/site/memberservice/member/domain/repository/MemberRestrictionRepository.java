package site.memberservice.member.domain.repository;

import site.memberservice.member.domain.Member;
import site.memberservice.member.domain.MemberRestriction;

import java.time.LocalDateTime;
import java.util.List;

public interface MemberRestrictionRepository {

    MemberRestriction save(MemberRestriction memberRestriction);

    List<MemberRestriction> findActiveByMember(Member member, LocalDateTime now);
}
