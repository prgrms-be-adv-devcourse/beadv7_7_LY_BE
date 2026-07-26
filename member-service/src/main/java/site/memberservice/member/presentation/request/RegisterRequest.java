package site.memberservice.member.presentation.request;

import site.memberservice.member.application.dto.MemberRegisterCommand;

public record RegisterRequest(
    String email,
    String password,
    String nickName,
    String name,
    String phoneNumber,
    String zipcode,
    String baseAddress,
    String detailAddress
) {
    public MemberRegisterCommand toCommand() {
        return new MemberRegisterCommand(
            email,
            password,
            nickName,
            name,
            phoneNumber,
            zipcode,
            baseAddress,
            detailAddress
        );
    }
}
