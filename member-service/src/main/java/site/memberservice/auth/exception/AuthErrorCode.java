package site.memberservice.auth.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import site.common.exception.ErrorCode;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum AuthErrorCode implements ErrorCode {

    INVALID_AUTH_TOKEN(UNAUTHORIZED, "AERR-0001", "유효하지 않은 인증 토큰입니다."),
    ;

    private final HttpStatus status;
    private final String value;
    private final String message;
}
