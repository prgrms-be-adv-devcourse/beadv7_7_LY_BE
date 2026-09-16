package site.memberservice.auth.domain;

public record RefreshTokenClaims(Long memberId, LoginType loginType) {
}
