package site.memberservice.auth.domain;

public interface AuthTokenProvider {

    AuthToken createAccessToken(Long memberId);

    Long validateAccessToken(AuthToken token);

    AuthToken createRefreshToken(Long memberId);

    Long validateRefreshToken(AuthToken token);
}
