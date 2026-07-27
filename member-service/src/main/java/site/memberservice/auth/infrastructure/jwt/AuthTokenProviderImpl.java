package site.memberservice.auth.infrastructure.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import site.memberservice.auth.domain.AuthToken;
import site.memberservice.auth.domain.AuthTokenProvider;

import javax.crypto.SecretKey;
import java.util.Date;

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
            .signWith(secretKey)
            .compact();

        return new AuthToken(jwtValue);
    }
}
