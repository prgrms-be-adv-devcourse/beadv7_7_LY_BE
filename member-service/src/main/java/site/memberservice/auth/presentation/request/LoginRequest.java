package site.memberservice.auth.presentation.request;

import site.memberservice.auth.application.dto.LoginCommand;
import site.memberservice.member.domain.Email;

public record LoginRequest(String email, String password) {

    public LoginCommand toCommand() {
        return new LoginCommand(
            new Email(email),
            password
        );
    }
}
