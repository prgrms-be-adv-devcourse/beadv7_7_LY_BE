package site.memberservice.auth.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import site.common.exception.ErrorCode;

import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum AuthErrorCode implements ErrorCode {

    INVALID_AUTH_TOKEN(UNAUTHORIZED, "AERR-0001", "유효하지 않은 인증 토큰입니다."),
    EXPIRED_AUTH_TOKEN(UNAUTHORIZED, "AERR-0002", "이미 만료된 인증 토큰입니다."),
    INVALID_CREDENTIALS(UNAUTHORIZED, "AERR-0003", "이메일 또는 비밀번호가 올바르지 않습니다."),
    LOGIN_CONCURRENCY_EXCEEDED(SERVICE_UNAVAILABLE, "AERR-0004", "로그인 요청이 몰려 처리할 수 없습니다. 잠시 후 다시 시도해주세요."),
    ;

    private final HttpStatus status;
    private final String value;
    private final String message;
}
