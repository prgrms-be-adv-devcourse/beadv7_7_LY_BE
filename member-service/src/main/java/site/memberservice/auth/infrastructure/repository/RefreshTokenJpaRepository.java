package site.memberservice.auth.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.memberservice.auth.domain.RefreshToken;

import java.util.Optional;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByMemberId(Long memberId);

    void deleteAllByMemberId(Long memberId);
}
