package site.memberservice.member.application.dto;

public record MemberRegisterCommand(
    String email,
    String password,
    String nickName,
    String name,
    String phoneNumber,
    String zipcode,
    String baseAddress,
    String detailAddress
) {
}
