package site.memberservice.member.application.dto;

import site.memberservice.member.domain.RestrictionType;

import java.time.LocalDateTime;

public record RestrictMemberCommand(
    Long memberId,
    RestrictionType restrictionType,
    String reason,
    LocalDateTime restrictedAt,
    LocalDateTime restrictedUntil
) {
}
