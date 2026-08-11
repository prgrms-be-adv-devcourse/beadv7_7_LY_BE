package site.memberservice.auth.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.memberservice.auth.domain.RefreshToken;
import site.memberservice.auth.domain.repository.RefreshTokenRepository;

import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository refreshTokenJpaRepository;

    @Override
    public RefreshToken save(final RefreshToken refreshToken) {
        return refreshTokenJpaRepository.save(refreshToken);
    }

    @Override
    public Optional<RefreshToken> findByMemberId(final Long memberId) {
        return refreshTokenJpaRepository.findByMemberId(memberId);
    }

    @Override
    public void deleteAllByMemberId(final Long memberId) {
        refreshTokenJpaRepository.deleteAllByMemberId(memberId);
    }
}
