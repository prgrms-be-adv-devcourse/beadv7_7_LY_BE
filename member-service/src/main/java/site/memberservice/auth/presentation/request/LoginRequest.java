package site.memberservice.auth.presentation.request;

import site.memberservice.auth.application.dto.LoginCommand;

public record LoginRequest(String email, String password, boolean keepLoggedIn) {

    public LoginCommand toCommand() {
        return new LoginCommand(
            email,
            password
        );
    }
}
