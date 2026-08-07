package site.pointwalletservice.ledger.exception;

import site.common.exception.BusinessException;
import site.common.exception.ErrorCode;

public class LedgerException extends BusinessException {
    public LedgerException(ErrorCode errorCode) {
        super(errorCode);
    }
}