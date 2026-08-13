package site.memberservice.member.application.dto;

import site.memberservice.member.domain.MemberRestriction;
import site.memberservice.member.domain.RestrictionType;

import java.time.LocalDateTime;

public record MemberRestrictionDto(
    RestrictionType restrictionType,
    String reason,
    LocalDateTime restrictedAt,
    LocalDateTime restrictedUntil
) {
    public static MemberRestrictionDto from(final MemberRestriction memberRestriction) {
        return new MemberRestrictionDto(
            memberRestriction.getRestrictionType(),
            memberRestriction.getReason(),
            memberRestriction.getRestrictedAt(),
            memberRestriction.getRestrictedUntil()
        );
    }
}
