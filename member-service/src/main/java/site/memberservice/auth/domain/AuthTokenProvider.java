package site.memberservice.auth.domain;

public interface AuthTokenProvider {

    AuthToken createAccessToken(Long memberId);

    Long validateAccessToken(AuthToken token);

    AuthToken createRefreshToken(Long memberId);

    AuthToken createRefreshToken(Long memberId, long validTime);

    Long validateRefreshToken(AuthToken token);
}
