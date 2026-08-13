package site.memberservice.member.presentation.response;

import site.memberservice.member.application.dto.MemberRestrictionDto;

import java.util.List;

public record RestrictionsResponse(
    Long memberId,
    boolean isRestricted,
    List<RestrictionResponse> restrictions
) {
    public static RestrictionsResponse of(final Long memberId, final List<MemberRestrictionDto> memberRestrictions) {
        final boolean isRestricted = !memberRestrictions.isEmpty();
        final List<RestrictionResponse> restrictionResponses = memberRestrictions.stream()
            .map(RestrictionResponse::from)
            .toList();

        return new RestrictionsResponse(memberId, isRestricted, restrictionResponses);
    }
}
