package site.coreservice.auction.exception;

import site.common.exception.BusinessException;
import site.common.exception.ErrorCode;

public class InvalidValueException extends BusinessException {

    public InvalidValueException(ErrorCode errorCode) {
        super(errorCode);
    }

    public InvalidValueException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
