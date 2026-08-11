package site.memberservice.auth.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.memberservice.auth.domain.RefreshToken;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshToken, Long> {

    boolean existsByValueAndMemberId(String value, Long memberId);

    void deleteAllByMemberId(Long memberId);
}
