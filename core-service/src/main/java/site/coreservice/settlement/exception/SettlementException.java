package site.coreservice.settlement.exception;

import site.common.exception.BusinessException;
import site.common.exception.ErrorCode;

public class SettlementException extends BusinessException {
    public SettlementException(ErrorCode errorCode) {
        super(errorCode);
    }
}
