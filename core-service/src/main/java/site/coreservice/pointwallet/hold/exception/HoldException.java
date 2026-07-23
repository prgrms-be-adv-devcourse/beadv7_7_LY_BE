package site.coreservice.pointwallet.hold.exception;

import site.common.exception.BusinessException;
import site.common.exception.ErrorCode;

public class HoldException extends BusinessException {
    public HoldException(ErrorCode errorCode) {
        super(errorCode);
    }
}