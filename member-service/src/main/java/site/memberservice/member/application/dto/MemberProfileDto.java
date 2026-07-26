package site.memberservice.member.application.dto;

import site.memberservice.member.domain.Member;

// 우선 회원 이메일, 닉네임만 포함시키고 추가로 필요한 정보가 있으면 검토 후 추가
public record MemberProfileDto(String email, String nickname) {
    public static MemberProfileDto from(final Member member) {
        return new MemberProfileDto(member.getEmail().getValue(), member.getNickname());
    }
}
