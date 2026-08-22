package site.auctionservice.exception;

import site.common.exception.BusinessException;
import site.common.exception.ErrorCode;

public class AuctionException extends BusinessException {
    public AuctionException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AuctionException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, errorCode.getMessage(), cause);
    }
}
