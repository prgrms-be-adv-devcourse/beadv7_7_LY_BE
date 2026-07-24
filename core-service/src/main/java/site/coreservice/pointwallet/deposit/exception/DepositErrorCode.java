package site.coreservice.pointwallet.deposit.exception;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import site.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum DepositErrorCode implements ErrorCode {

    ALREADY_PROCESSED_DEPOSIT(HttpStatus.BAD_REQUEST, "DERR-3001", "이미 처리된 충전 요청입니다."),
    ORDER_ID_MISMATCH(HttpStatus.BAD_REQUEST, "DERR-3002", "요청 시점과 다른 주문번호입니다."),
    AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "DERR-3003", "요청 금액과 승인 금액이 일치하지 않습니다."),
    DEPOSIT_NOT_FOUND(HttpStatus.NOT_FOUND, "DERR-3004", "충전 요청 내역을 찾을 수 없습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "DERR-3005", "요청 값이 올바르지 않습니다."),
    CANCEL_INSUFFICIENT_BALANCE(HttpStatus.UNPROCESSABLE_CONTENT, "DERR-3006", "이미 사용된 예치금이 있어 취소할 수 없습니다."),
    WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "DERR-3007", "지갑을 찾을 수 없습니다."),
    PG_API_ERROR(HttpStatus.BAD_GATEWAY, "DERR-3008", "결제 대행사 처리 중 오류가 발생했습니다."),
    ;

    private final HttpStatus status;
    private final String value;
    private final String message;
}