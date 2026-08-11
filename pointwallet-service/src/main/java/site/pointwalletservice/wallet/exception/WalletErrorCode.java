package site.pointwalletservice.wallet.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import site.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum WalletErrorCode implements ErrorCode {

    INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "WERR-3001", "잔액이 부족합니다."),
    WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "WERR-3002", "지갑을 찾을 수 없습니다."),
    LOCK_ACQUISITION_FAILED(HttpStatus.CONFLICT, "WERR-3003", "지갑 접근이 몰려 처리할 수 없습니다. 잠시 후 다시 시도해주세요."),
    ;

    private final HttpStatus status;
    private final String value;
    private final String message;
}