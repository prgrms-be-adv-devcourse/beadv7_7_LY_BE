package site.auctionservice.exception;

import site.common.exception.BusinessException;
import site.common.exception.ErrorCode;

public class InfrastructureUnavailableException extends BusinessException {
    public InfrastructureUnavailableException(ErrorCode errorCode) {
        super(errorCode);
    }

    public InfrastructureUnavailableException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, errorCode.getMessage(), cause);
    }
}
