package site.memberservice.auth.application.dto;

public record LoginResult(String accessToken, String refreshToken) {
}
