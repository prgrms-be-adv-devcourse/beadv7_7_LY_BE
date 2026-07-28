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

    private final SecretKey secretKey;
    private final long tokenValidTime;

    public AuthTokenProviderImpl(
        @Value("${app.jwt.secret-key}") final String secretKey,
        @Value("${app.jwt.token-expiration-time}") final long tokenValidTime
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes());
        this.tokenValidTime = tokenValidTime;
    }

    // TODO : #80 파이널에서 Refresh Token 발급 + Access Token 만료시간 단축 고민 및 적용 예정
    @Override
    public AuthToken createToken(final Long memberId) {
        final Claims claims = Jwts.claims()
            .subject(memberId.toString())
            .build();
        final Date now = new Date();
        final Date validity = new Date(now.getTime() + tokenValidTime);

        final String jwtValue = Jwts.builder()
            .claims(claims)
            .issuedAt(now)
            .expiration(validity)
            .signWith(secretKey, Jwts.SIG.HS256)
            .compact();

        return new AuthToken(jwtValue);
    }

    @Override
    public Long validateToken(final AuthToken token) {
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
