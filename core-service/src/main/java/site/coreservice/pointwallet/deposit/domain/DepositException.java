package site.coreservice.pointwallet.deposit.domain;

import lombok.Getter;
import site.common.exception.ErrorCode;

@Getter
public class DepositException extends RuntimeException {

    private final ErrorCode errorCode;

    public DepositException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}