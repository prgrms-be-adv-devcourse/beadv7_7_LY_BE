package site.memberservice.auth.domain.repository;

import site.memberservice.auth.domain.RefreshToken;

public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken refreshToken);

    boolean existsByValueAndMemberId(final String value, final Long memberId);

    void deleteAllByMemberId(Long memberId);
}
