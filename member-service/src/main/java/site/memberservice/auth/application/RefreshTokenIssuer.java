package site.memberservice.auth.application;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import site.memberservice.auth.domain.RefreshToken;
import site.memberservice.auth.domain.repository.RefreshTokenRepository;

// 로그인의 DB 조회/쓰기 부분만 별도 빈으로 분리한 것 — AuthService.login()이 Argon2 검증을
// 트랜잭션 밖에서 먼저 끝낸 뒤, 이 빈의 프록시를 거쳐 호출하게 해서 커넥션 점유 시간을
// refreshToken upsert 구간으로만 한정한다. 같은 클래스 안의 private 메서드에 @Transactional을
// 붙이면 self-invocation 때문에 트랜잭션이 아예 시작되지 않아, 별도 빈으로 뺐다.
@RequiredArgsConstructor
@Service
public class RefreshTokenIssuer {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public RefreshToken upsert(final Long memberId, final String refreshTokenValue) {
        final RefreshToken refreshToken = refreshTokenRepository.findByMemberId(memberId)
            .map(existing -> {
                existing.updateValue(refreshTokenValue);
                return existing;
            })
            .orElseGet(() -> RefreshToken.create(refreshTokenValue, memberId));

        return refreshTokenRepository.save(refreshToken);
    }
}
