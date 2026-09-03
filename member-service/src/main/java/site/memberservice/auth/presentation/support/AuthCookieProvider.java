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

    public ResponseCookie createAccessTokenCookie(final String accessToken, final boolean keepLoggedIn) {
        return createCookie(ACCESS_TOKEN_COOKIE_NAME, accessToken, resolveMaxAge(accessTokenValidTime, keepLoggedIn));
    }

    public ResponseCookie createRefreshTokenCookie(final String refreshToken, final boolean keepLoggedIn) {
        return createCookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken, resolveMaxAge(refreshTokenValidTime, keepLoggedIn));
    }

    public ResponseCookie expireAccessTokenCookie() {
        return createCookie(ACCESS_TOKEN_COOKIE_NAME, "", Duration.ZERO);
    }

    public ResponseCookie expireRefreshTokenCookie() {
        return createCookie(REFRESH_TOKEN_COOKIE_NAME, "", Duration.ZERO);
    }

    private Duration resolveMaxAge(final long validTime, final boolean keepLoggedIn) {
        if (keepLoggedIn) {
            return Duration.ofMillis(validTime);
        }
        return null;
    }

    private ResponseCookie createCookie(final String name, final String value, final Duration maxAge) {
        final ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(true)
            .path("/")
            .sameSite("Lax");

        if (maxAge != null) {
            builder.maxAge(maxAge);
        }

        return builder.build();
    }
}
