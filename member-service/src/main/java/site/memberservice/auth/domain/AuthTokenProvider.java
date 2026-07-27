package site.memberservice.auth.domain;

public interface AuthTokenProvider {

    AuthToken createToken(Long memberId);
}
