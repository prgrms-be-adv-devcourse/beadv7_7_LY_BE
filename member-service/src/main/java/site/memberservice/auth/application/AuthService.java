package site.memberservice.auth.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import site.memberservice.auth.application.dto.LoginCommand;
import site.memberservice.auth.application.dto.LoginResult;
import site.memberservice.auth.domain.AuthToken;
import site.memberservice.auth.domain.AuthTokenProvider;
import site.memberservice.auth.domain.LoginType;
import site.memberservice.auth.domain.RefreshTokenClaims;
import site.memberservice.auth.exception.AuthException;
import site.memberservice.auth.infrastructure.redis.RefreshTokenStore;
import site.memberservice.member.application.MemberService;
import site.memberservice.member.domain.repository.MemberCredentials;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static site.memberservice.auth.exception.AuthErrorCode.INVALID_AUTH_TOKEN;
import static site.memberservice.auth.exception.AuthErrorCode.INVALID_CREDENTIALS;
import static site.memberservice.auth.exception.AuthErrorCode.LOGIN_CONCURRENCY_EXCEEDED;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthService {

    private static final long ARGON2_ACQUIRE_TIMEOUT_SECONDS = 7;

    private final MemberService memberService;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenProvider authTokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final Semaphore argon2ConcurrencyLimiter;

    public LoginResult login(final LoginCommand command) {
        final MemberCredentials credentials = memberService.findMemberCredentials(command.email())
            .orElseThrow(() -> new AuthException(INVALID_CREDENTIALS, format("존재하지 않는 회원의 이메일입니다. input: %s", command.email())));

        if (!matchesWithConcurrencyLimit(command.password(), credentials.password())) {
            throw new AuthException(INVALID_CREDENTIALS, "유효하지 않는 회원 비밀번호입니다.");
        }

        final Long memberId = credentials.id();
        final AuthToken accessToken = authTokenProvider.createAccessToken(memberId);
        final LoginType loginType = LoginType.from(command.keepLoggedIn());
        final String refreshTokenValue = authTokenProvider.createRefreshToken(memberId, loginType).getValue();

        refreshTokenStore.save(memberId, loginType, refreshTokenValue, authTokenProvider.resolveRefreshTokenValidTime(loginType));

        return new LoginResult(accessToken.getValue(), refreshTokenValue);
    }

    private boolean matchesWithConcurrencyLimit(final String rawPassword, final String encodedPassword) {
        final boolean acquired;
        try {
            acquired = argon2ConcurrencyLimiter.tryAcquire(ARGON2_ACQUIRE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AuthException(LOGIN_CONCURRENCY_EXCEEDED, "로그인 처리 중 인터럽트가 발생했습니다.", e);
        }

        if (!acquired) {
            throw new AuthException(LOGIN_CONCURRENCY_EXCEEDED, "로그인 요청이 몰려 처리할 수 없습니다. 잠시 후 다시 시도해주세요.");
        }

        try {
            return passwordEncoder.matches(rawPassword, encodedPassword);
        } finally {
            argon2ConcurrencyLimiter.release();
        }
    }

    public String reissueAccessToken(final String refreshTokenValue) {
        final AuthToken refreshToken = new AuthToken(refreshTokenValue);
        final RefreshTokenClaims claims = authTokenProvider.validateRefreshToken(refreshToken);

        if (!refreshTokenStore.matches(claims.memberId(), claims.loginType(), refreshToken.getValue())) {
            throw new AuthException(INVALID_AUTH_TOKEN, "유효하지 않은 리프레쉬 토큰 입니다.");
        }

        return authTokenProvider.createAccessToken(claims.memberId()).getValue();
    }

    public void logout(final Long memberId, final String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            return;
        }

        final RefreshTokenClaims claims;
        try {
            claims = authTokenProvider.validateRefreshToken(new AuthToken(refreshTokenValue));
        } catch (final AuthException e) {
            log.info("로그아웃 시 refreshToken 검증에 실패해 Redis 삭제를 건너뜁니다. memberId={}", memberId, e);
            return;
        }

        if (!claims.memberId().equals(memberId)) {
            log.warn("로그아웃 요청자와 refreshToken 소유자가 일치하지 않아 Redis 삭제를 건너뜁니다. requesterId={}", memberId);
            return;
        }

        refreshTokenStore.remove(claims.memberId(), claims.loginType(), refreshTokenValue);
    }
}
