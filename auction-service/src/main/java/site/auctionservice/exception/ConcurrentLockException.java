package site.auctionservice.exception;

import site.common.exception.BusinessException;

public class ConcurrentLockException extends BusinessException {
    public ConcurrentLockException() {
        super(AuctionErrorCode.CONCURRENT_LOCK);
    }

    public ConcurrentLockException(String message) {
        super(AuctionErrorCode.CONCURRENT_LOCK, message);
    }

    public ConcurrentLockException(String message, Throwable cause) {
        super(AuctionErrorCode.CONCURRENT_LOCK, message, cause);
    }
}
