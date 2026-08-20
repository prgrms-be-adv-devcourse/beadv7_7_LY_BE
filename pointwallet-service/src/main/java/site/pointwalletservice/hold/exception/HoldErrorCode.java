package site.pointwalletservice.hold.exception;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import site.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum HoldErrorCode implements ErrorCode {

    WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "HERR-3001", "지갑을 찾을 수 없습니다."),
    INSUFFICIENT_BALANCE(HttpStatus.UNPROCESSABLE_CONTENT, "HERR-3002", "예치금이 부족합니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "HERR-3003", "요청 값이 올바르지 않습니다."),
    HOLD_NOT_FOUND(HttpStatus.NOT_FOUND, "HERR-3004", "홀드를 찾을 수 없습니다."),
    LOCK_ACQUISITION_FAILED(HttpStatus.CONFLICT, "HERR-3005", "입찰이 몰려 처리할 수 없습니다. 잠시 후 다시 시도해주세요."),
    HOLD_ALREADY_FINALIZED(HttpStatus.CONFLICT, "HERR-3006", "이미 교체되었거나 소멸되어 롤백할 수 없는 홀드입니다."),
    HOLD_MISMATCH(HttpStatus.CONFLICT, "HERR-3007", "요청한 홀드 정보가 서버 기록과 일치하지 않습니다."),
    ;

    private final HttpStatus status;
    private final String value;
    private final String message;
}