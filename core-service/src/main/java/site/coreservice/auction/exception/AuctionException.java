package site.coreservice.auction.exception;

import site.common.exception.BusinessException;
import site.common.exception.ErrorCode;

public class AuctionException extends BusinessException {
    public AuctionException(ErrorCode errorCode) {
        super(errorCode);
    }
}
