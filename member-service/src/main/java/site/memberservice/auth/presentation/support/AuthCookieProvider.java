package site.memberservice.auth.presentation.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AuthCookieProvider {

    public static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    @Value("${app.jwt.access-token-expiration-time}")
    private long accessTokenValidTime;

    @Value("${app.jwt.refresh-token-expiration-time}")
    private long refreshTokenValidTime;

    public ResponseCookie createAccessTokenCookie(final String accessToken) {
        return createCookie(ACCESS_TOKEN_COOKIE_NAME, accessToken, Duration.ofMillis(accessTokenValidTime));
    }

    public ResponseCookie createRefreshTokenCookie(final String refreshToken) {
        return createCookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken, Duration.ofMillis(refreshTokenValidTime));
    }

    public ResponseCookie expireAccessTokenCookie() {
        return createCookie(ACCESS_TOKEN_COOKIE_NAME, "", Duration.ZERO);
    }

    public ResponseCookie expireRefreshTokenCookie() {
        return createCookie(REFRESH_TOKEN_COOKIE_NAME, "", Duration.ZERO);
    }

    private ResponseCookie createCookie(final String name, final String value, final Duration maxAge) {
        return ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(maxAge)
            .sameSite("Lax")
            .build();
    }
}
