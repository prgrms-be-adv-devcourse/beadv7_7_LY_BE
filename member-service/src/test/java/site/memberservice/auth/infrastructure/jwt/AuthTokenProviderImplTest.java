package site.memberservice.auth.infrastructure.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.memberservice.auth.domain.AuthToken;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class AuthTokenProviderImplTest {

    private static final String TEST_SECRET_KEY = "testSecretKeyForJwtTokenProviderImplTest123456789";
    private static final long TOKEN_VALID_TIME = 1000L * 60 * 60;   // 1시간

    private AuthTokenProviderImpl authTokenProvider;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        this.authTokenProvider = new AuthTokenProviderImpl(TEST_SECRET_KEY, TOKEN_VALID_TIME);
        this.secretKey = Keys.hmacShaKeyFor(TEST_SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    @DisplayName("회원의 인증 토큰을 생성한다.")
    @Test
    void createToken() {
        // Given
        final Long memberId = 1727L;

        // When
        final AuthToken authToken = authTokenProvider.createToken(memberId);

        // Then
        assertThat(authToken).isNotNull();

        final Claims claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(authToken.getValue())
            .getPayload();

        assertThat(claims.getSubject()).isEqualTo(memberId.toString());
    }
}
