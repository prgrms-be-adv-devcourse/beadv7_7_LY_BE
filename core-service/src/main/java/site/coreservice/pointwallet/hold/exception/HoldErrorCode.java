package site.coreservice.pointwallet.hold.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import site.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum HoldErrorCode implements ErrorCode {

    WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "HOLD-3001", "지갑을 찾을 수 없습니다."),
    INSUFFICIENT_BALANCE(HttpStatus.UNPROCESSABLE_CONTENT, "HOLD-3002", "예치금이 부족합니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "HOLD-3003", "요청 값이 올바르지 않습니다."),
    ;

    private final HttpStatus status;
    private final String value;
    private final String message;
}