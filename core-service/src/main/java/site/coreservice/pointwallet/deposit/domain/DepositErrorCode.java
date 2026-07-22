package site.coreservice.pointwallet.deposit.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import site.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum DepositErrorCode implements ErrorCode {

    ALREADY_PROCESSED_DEPOSIT("DEPOSIT-0001", "이미 처리된 충전 요청입니다."),
    ORDER_ID_MISMATCH("DEPOSIT-0002", "요청 시점과 다른 주문번호입니다."),
    AMOUNT_MISMATCH("DEPOSIT-0003", "요청 금액과 승인 금액이 일치하지 않습니다."),
    DEPOSIT_NOT_FOUND("DEPOSIT-0004", "충전 요청 내역을 찾을 수 없습니다."),
    INVALID_REQUEST("DEPOSIT-0005", "요청 값이 올바르지 않습니다."),
    ;

    private final String value;
    private final String message;
}