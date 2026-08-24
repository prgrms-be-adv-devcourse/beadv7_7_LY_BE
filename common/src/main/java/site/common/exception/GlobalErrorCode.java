package site.common.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum GlobalErrorCode implements ErrorCode {

    INTERNAL_SERVER_APPLICATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GERR-0001",
        "서버 애플리케이션에 예기치 못한 문제가 발생했습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "GERR-0002", "존재하지 않는 리소스입니다."),
    INVALID_ARGUMENT(HttpStatus.BAD_REQUEST, "GERR-0003", "잘못된 요청 값입니다."),

    // inner
    MEMBER_ID_HEADER_REQUIRED(HttpStatus.BAD_REQUEST, "INER-0001", "X-Member-Id 헤더가 필요합니다.");

    private final HttpStatus status;
    private final String value;
    private final String message;
}
