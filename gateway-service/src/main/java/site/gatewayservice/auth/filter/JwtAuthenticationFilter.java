package site.gatewayservice.auth.filter;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import site.common.exception.ErrorCode;
import site.common.response.ApiResponse;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static site.gatewayservice.auth.exception.AuthErrorCode.EXPIRED_AUTH_TOKEN;
import static site.gatewayservice.auth.exception.AuthErrorCode.INVALID_AUTH_TOKEN;

@Slf4j
@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SecretKey secretKey;

    public JwtAuthenticationFilter(@Value("${app.jwt.secret-key}") final String secretKeyValue) {
        this.secretKey = Keys.hmacShaKeyFor(secretKeyValue.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public GatewayFilter apply(final Object config) {
        return (exchange, chain) -> {
            final String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

            String token = null;

            if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7); // "Bearer " (7자) 이후의 값만 추출
            }

            if (token == null || token.isBlank()) {
                log.info("인증 토큰이 헤더에 존재하지 않습니다.");
                return makeErrorResponse(exchange.getResponse(), INVALID_AUTH_TOKEN);
            }

            // TODO: Token이 존재하지 않을 경우 에러 응답 보내는 로직 작성

            try {
                final String subject = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();

                return chain.filter(
                    exchange.mutate()
                        .request(
                            exchange.getRequest()
                                .mutate()
                                .header("X-Member-Id", subject)
                                .headers(httpHeaders -> httpHeaders.remove("Authorization"))
                                .build()
                        )
                        .build()
                );
            } catch (final ExpiredJwtException e) {
                log.info("이미 만료된 인증 토큰입니다.");
                return makeErrorResponse(exchange.getResponse(), EXPIRED_AUTH_TOKEN);
            } catch (final JwtException e) {
                log.info("유효하지 않은 인증 토큰입니다.");
                return makeErrorResponse(exchange.getResponse(), INVALID_AUTH_TOKEN);
            }
        };
    }

    private Mono<Void> makeErrorResponse(final ServerHttpResponse response, final ErrorCode errorCode) {
        response.setStatusCode(errorCode.getStatus());
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        final byte[] bytes = objectMapper.writeValueAsBytes(ApiResponse.fail(errorCode));
        final DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
