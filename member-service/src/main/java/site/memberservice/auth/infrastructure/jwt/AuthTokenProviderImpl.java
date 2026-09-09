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
import site.memberservice.auth.domain.LoginType;
import site.memberservice.auth.domain.RefreshTokenClaims;
import site.memberservice.auth.exception.AuthException;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;

import static java.lang.String.format;
import static site.memberservice.auth.exception.AuthErrorCode.EXPIRED_AUTH_TOKEN;
import static site.memberservice.auth.exception.AuthErrorCode.INVALID_AUTH_TOKEN;

@Component
public class AuthTokenProviderImpl implements AuthTokenProvider {

    private static final String LOGIN_TYPE_CLAIM = "loginType";
    private static final long PUBLIC_PC_REFRESH_TOKEN_VALID_TIME = Duration.ofHours(12).toMillis();

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
        final Claims claims = Jwts.claims()
            .subject(memberId.toString())
            .build();
        return generateToken(claims, accessTokenSecretKey, accessTokenValidTime);
    }

    @Override
    public AuthToken createRefreshToken(final Long memberId, final LoginType loginType) {
        final Claims claims = Jwts.claims()
            .subject(memberId.toString())
            .add(LOGIN_TYPE_CLAIM, loginType.name())
            .build();
        return generateToken(claims, refreshTokenSecretKey, resolveRefreshTokenValidTime(loginType));
    }

    @Override
    public long resolveRefreshTokenValidTime(final LoginType loginType) {
        return loginType == LoginType.PUBLIC_PC ? PUBLIC_PC_REFRESH_TOKEN_VALID_TIME : refreshTokenValidTime;
    }

    private AuthToken generateToken(final Claims claims, final SecretKey secretKey, final long validTime) {
        final Date now = new Date();
        final Date validity = new Date(now.getTime() + validTime);

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
        final Claims claims = parseClaims(token, accessTokenSecretKey);
        return Long.parseLong(claims.getSubject());
    }

    @Override
    public RefreshTokenClaims validateRefreshToken(final AuthToken token) {
        final Claims claims = parseClaims(token, refreshTokenSecretKey);
        final Long memberId = Long.parseLong(claims.getSubject());
        final LoginType loginType = parseLoginType(claims);
        return new RefreshTokenClaims(memberId, loginType);
    }

    private LoginType parseLoginType(final Claims claims) {
        try {
            return LoginType.valueOf(claims.get(LOGIN_TYPE_CLAIM, String.class));
        } catch (final IllegalArgumentException | NullPointerException e) {
            throw new AuthException(INVALID_AUTH_TOKEN, "유효하지 않은 인증 토큰입니다.", e);
        }
    }

    private Claims parseClaims(final AuthToken token, final SecretKey secretKey) {
        try {
            return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token.getValue())
                .getPayload();
        } catch (final ExpiredJwtException e) {
            final Date expiration = e.getClaims().getExpiration();
            throw new AuthException(EXPIRED_AUTH_TOKEN, format("이미 만료된 인증 토큰입니다. 토큰 만료일: %s", expiration));
        } catch (final JwtException e) {
            throw new AuthException(INVALID_AUTH_TOKEN, "유효하지 않은 인증 토큰입니다.", e);
        }
    }
}
