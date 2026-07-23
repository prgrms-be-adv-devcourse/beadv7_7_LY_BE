package site.coreservice.pointwallet.deposit.exception;

import site.common.exception.BusinessException;
import site.common.exception.ErrorCode;

public class DepositException extends BusinessException {
    public DepositException(ErrorCode errorCode) {
        super(errorCode);
    }
}