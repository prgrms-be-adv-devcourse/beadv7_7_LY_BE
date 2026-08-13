package site.memberservice.member.presentation.response;

import site.memberservice.member.application.dto.MemberRestrictionDto;
import site.memberservice.member.domain.RestrictionType;

import java.time.LocalDateTime;

public record RestrictionResponse(
    RestrictionType type,
    String reason,
    LocalDateTime startedAt,
    LocalDateTime expiresAt
) {
    public static RestrictionResponse from(final MemberRestrictionDto memberRestrictionDto) {
        return new RestrictionResponse(
            memberRestrictionDto.restrictionType(),
            memberRestrictionDto.reason(),
            memberRestrictionDto.restrictedAt(),
            memberRestrictionDto.restrictedUntil()
        );
    }
}
