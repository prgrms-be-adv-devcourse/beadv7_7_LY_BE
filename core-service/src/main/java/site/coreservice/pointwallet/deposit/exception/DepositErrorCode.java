package site.coreservice.pointwallet.deposit.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import site.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum DepositErrorCode implements ErrorCode {

    ALREADY_PROCESSED_DEPOSIT(HttpStatus.BAD_REQUEST, "DEPOSIT-0001", "이미 처리된 충전 요청입니다."),
    ORDER_ID_MISMATCH(HttpStatus.BAD_REQUEST, "DEPOSIT-0002", "요청 시점과 다른 주문번호입니다."),
    AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "DEPOSIT-0003", "요청 금액과 승인 금액이 일치하지 않습니다."),
    DEPOSIT_NOT_FOUND(HttpStatus.NOT_FOUND, "DEPOSIT-0004", "충전 요청 내역을 찾을 수 없습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "DEPOSIT-0005", "요청 값이 올바르지 않습니다."),
    ;

    private final HttpStatus status;
    private final String value;
    private final String message;
}