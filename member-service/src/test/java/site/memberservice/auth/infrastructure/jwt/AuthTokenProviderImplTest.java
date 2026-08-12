package site.memberservice.auth.infrastructure.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.memberservice.auth.domain.AuthToken;
import site.memberservice.auth.exception.AuthException;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JWT 프로바이더 객체")
class AuthTokenProviderImplTest {

    private static final String TEST_SECRET_KEY = "testSecretKeyForJwtTokenProviderImplTest123456789";
    private static final long TOKEN_VALID_TIME = 1000L * 60 * 60;   // 1시간

    private AuthTokenProviderImpl authTokenProvider;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        this.authTokenProvider = new AuthTokenProviderImpl(TEST_SECRET_KEY, TEST_SECRET_KEY, TOKEN_VALID_TIME, TOKEN_VALID_TIME);
        this.secretKey = Keys.hmacShaKeyFor(TEST_SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    @DisplayName("회원의 인증 토큰을 생성한다.")
    @Test
    void createAccessToken() {
        // Given
        final Long memberId = 1727L;

        // When
        final AuthToken accessToken = authTokenProvider.createAccessToken(memberId);
        final AuthToken refreshToken = authTokenProvider.createRefreshToken(memberId);

        // Then
        assertThat(accessToken).isNotNull();
        assertThat(refreshToken).isNotNull();

        final Claims accessTokenClaims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(accessToken.getValue())
            .getPayload();

        final Claims refreshTokenClaims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(refreshToken.getValue())
            .getPayload();

        assertThat(accessTokenClaims.getSubject()).isEqualTo(memberId.toString());
        assertThat(refreshTokenClaims.getSubject()).isEqualTo(memberId.toString());
    }

    @DisplayName("유효한 토큰이 입력되면 회원 id 값을 반환한다.")
    @Test
    void validateToken() {
        // Given
        final Long memberId = 1727L;
        final AuthToken accessToken = authTokenProvider.createAccessToken(memberId);
        final AuthToken refreshToken = authTokenProvider.createRefreshToken(memberId);

        // When
        final Long authenticatedMemberIdFromAccessToken = authTokenProvider.validateAccessToken(accessToken);
        final Long authenticatedMemberIdFromRefreshToken = authTokenProvider.validateRefreshToken(refreshToken);

        // Then
        assertThat(authenticatedMemberIdFromAccessToken).isEqualTo(memberId);
        assertThat(authenticatedMemberIdFromRefreshToken).isEqualTo(memberId);
    }

    @DisplayName("만료된 인증 토큰이 입력되면 예외가 발생한다.")
    @Test
    void throwExceptionWhenInputExpiredToken() {
        // Given
        final Long memberId = 1727L;
        final AuthTokenProviderImpl expiredTokenTestProvider = new AuthTokenProviderImpl(
            TEST_SECRET_KEY,
            TEST_SECRET_KEY,
            -1000000,
            -1000000
        );
        final AuthToken expiredToken = expiredTokenTestProvider.createAccessToken(memberId);

        // When & Then
        assertThatThrownBy(() -> expiredTokenTestProvider.validateAccessToken(expiredToken))
            .isInstanceOf(AuthException.class)
            .hasMessageStartingWith("이미 만료된 인증 토큰입니다. 토큰 만료일:");
    }

    @DisplayName("유효하지 않은 인증 토큰이 입력되면 예외가 발생한다.")
    @Test
    void throwExceptionWhenInvalidToken() {
        // Given
        final AuthToken invalidToken = new AuthToken("AAAABBBBCCCCDDDDTTTTEEEESSSSTTTT");

        // When & Then
        assertThatThrownBy(() -> authTokenProvider.validateAccessToken(invalidToken))
            .isInstanceOf(AuthException.class)
            .hasMessageStartingWith("유효하지 않은 인증 토큰입니다.");
    }
}
