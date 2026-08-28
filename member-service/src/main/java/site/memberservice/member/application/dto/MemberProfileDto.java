package site.memberservice.member.application.dto;

import site.memberservice.member.domain.Member;

public record MemberProfileDto(Long id, String email, String nickname) {
    public static MemberProfileDto from(final Member member) {
        return new MemberProfileDto(member.getId(), member.getEmail().getValue(), member.getNickname());
    }
}
