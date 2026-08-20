package site.pointwalletservice.ledger.exception;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import site.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum LedgerErrorCode implements ErrorCode {

    INVALID_TRANSACTION_TYPE(HttpStatus.BAD_REQUEST, "LERR-3001", "유효하지 않은 거래 유형입니다."),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "LERR-3002", "from은 to보다 늦을 수 없습니다."),
    ;

    private final HttpStatus status;
    private final String value;
    private final String message;
}