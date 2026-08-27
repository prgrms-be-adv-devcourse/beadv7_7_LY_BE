package site.pointwalletservice.withdraw.exception;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import site.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum WithdrawErrorCode implements ErrorCode {

    WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "WDERR-3001", "지갑을 찾을 수 없습니다."),
    INSUFFICIENT_BALANCE(HttpStatus.UNPROCESSABLE_CONTENT, "WDERR-3002", "예치금이 부족합니다."),
    BANK_ACCOUNT_NOT_FOUND(HttpStatus.BAD_REQUEST, "WDERR-3003", "등록된 계좌 정보가 없습니다."),
    WITHDRAW_NOT_FOUND(HttpStatus.NOT_FOUND, "WDERR-3004", "인출 신청 내역을 찾을 수 없습니다."),
    ALREADY_PROCESSED(HttpStatus.CONFLICT, "WDERR-3005", "이미 처리된 인출 신청입니다."),
    LOCK_ACQUISITION_FAILED(HttpStatus.CONFLICT, "WDERR-3006", "인출 요청이 몰려 처리할 수 없습니다. 잠시 후 다시 시도해주세요."),
    IDEMPOTENCY_KEY_REQUIRED(HttpStatus.BAD_REQUEST, "WDERR-3007", "멱등키(Idempotency-Key) 헤더가 비어있거나 형식이 올바르지 않습니다."),
    ;

    private final HttpStatus status;
    private final String value;
    private final String message;
}