package site.memberservice.auth.infrastructure.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import site.memberservice.auth.domain.AuthToken;
import site.memberservice.auth.domain.AuthTokenProvider;
import site.memberservice.auth.exception.AuthException;

import javax.crypto.SecretKey;
import java.util.Date;

import static java.lang.String.format;
import static site.memberservice.auth.exception.AuthErrorCode.EXPIRED_AUTH_TOKEN;
import static site.memberservice.auth.exception.AuthErrorCode.INVALID_AUTH_TOKEN;

@Component
public class AuthTokenProviderImpl implements AuthTokenProvider {

    private final SecretKey accessTokenSecretKey;
    private final SecretKey refreshTokenSecretKey;
    private final long accessTokenValidTime;
    private final long refreshTokenValidTime;

    public AuthTokenProviderImpl(
        @Value("${app.jwt.access-token-secret-key}") final String accessTokenSecretKey,
        @Value("${app.jwt.refresh-token-secret-key}") final String refreshTokenSecretKey,
        @Value("${app.jwt.access-token-expiration-time}") final long accessTokenValidTime,
        @Value("${app.jwt.refresh-token-expiration-time}") final long refreshTokenValidTime
    ) {
        this.accessTokenSecretKey = Keys.hmacShaKeyFor(accessTokenSecretKey.getBytes());
        this.refreshTokenSecretKey = Keys.hmacShaKeyFor(refreshTokenSecretKey.getBytes());
        this.accessTokenValidTime = accessTokenValidTime;
        this.refreshTokenValidTime = refreshTokenValidTime;
    }

    @Override
    public AuthToken createAccessToken(final Long memberId) {
        return generateToken(memberId, accessTokenSecretKey, accessTokenValidTime);
    }

    @Override
    public AuthToken createRefreshToken(final Long memberId) {
        return generateToken(memberId, refreshTokenSecretKey, refreshTokenValidTime);
    }

    private AuthToken generateToken(final Long memberId, final SecretKey secretKey, final long accessTokenValidTime) {
        final Claims claims = Jwts.claims()
            .subject(memberId.toString())
            .build();
        final Date now = new Date();
        final Date validity = new Date(now.getTime() + accessTokenValidTime);

        final String jwtValue = Jwts.builder()
            .claims(claims)
            .issuedAt(now)
            .expiration(validity)
            .signWith(secretKey, Jwts.SIG.HS256)
            .compact();

        return new AuthToken(jwtValue);
    }

    @Override
    public Long validateAccessToken(final AuthToken token) {
        return validateToken(token, accessTokenSecretKey);
    }

    @Override
    public Long validateRefreshToken(final AuthToken token) {
        return validateToken(token, refreshTokenSecretKey);
    }

    private Long validateToken(final AuthToken token, final SecretKey secretKey) {
        try {
            final Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token.getValue())
                .getPayload();
            return Long.parseLong(claims.getSubject());
        } catch (final ExpiredJwtException e) {
            final Date expiration = e.getClaims().getExpiration();
            throw new AuthException(EXPIRED_AUTH_TOKEN, format("이미 만료된 인증 토큰입니다. 토큰 만료일: %s", expiration));
        } catch (final JwtException e) {
            throw new AuthException(INVALID_AUTH_TOKEN, "유효하지 않은 인증 토큰입니다.", e);
        }
    }
}
