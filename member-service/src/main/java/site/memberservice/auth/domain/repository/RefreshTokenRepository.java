package site.memberservice.auth.domain.repository;

import site.memberservice.auth.domain.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken refreshToken);

    Optional<RefreshToken> findByMemberId(Long memberId);

    void deleteByMemberId(Long memberId);
}
