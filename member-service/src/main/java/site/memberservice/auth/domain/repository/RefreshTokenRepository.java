package site.memberservice.auth.domain.repository;

import site.memberservice.auth.domain.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken refreshToken);

    Optional<RefreshToken> findByMemberId(Long memberId);

    boolean existsByValueAndMemberId(final String value, final Long memberId);

    void deleteAllByMemberId(Long memberId);
}
