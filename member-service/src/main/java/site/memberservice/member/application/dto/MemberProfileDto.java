package site.memberservice.member.application.dto;

import site.memberservice.member.domain.repository.MemberProfileView;

public record MemberProfileDto(Long id, String email, String nickname) {
    public static MemberProfileDto from(final MemberProfileView view) {
        return new MemberProfileDto(view.id(), view.email(), view.nickname());
    }
}
