package site.pointwalletservice.wallet.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import site.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum WalletErrorCode implements ErrorCode {

    INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "WALLET-3001", "잔액이 부족합니다."),
    WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "WALLET-3002", "지갑을 찾을 수 없습니다."),
    ;

    private final HttpStatus status;
    private final String value;
    private final String message;
}