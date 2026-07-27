package site.coreservice.settlement.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import site.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum SettlementErrorCode implements ErrorCode {

    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "SERR-7001", "from은 to보다 늦을 수 없습니다."),
    INVALID_STATUS(HttpStatus.BAD_REQUEST, "SERR-7002", "유효하지 않은 정산 상태입니다."),
    ;

    private final HttpStatus status;
    private final String value;
    private final String message;
}
