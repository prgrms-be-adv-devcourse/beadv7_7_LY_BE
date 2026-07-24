package site.coreservice.pointwallet.ledger.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import site.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum LedgerErrorCode implements ErrorCode {

    INVALID_TRANSACTION_TYPE(HttpStatus.BAD_REQUEST, "LEDGER-3001", "유효하지 않은 거래 유형입니다."),
    ;

    private final HttpStatus status;
    private final String value;
    private final String message;
}