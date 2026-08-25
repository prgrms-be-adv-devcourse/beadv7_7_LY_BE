package site.auctionservice.exception;

import site.common.exception.BusinessException;

public class LockAcquisitionFailedException extends BusinessException {
    public LockAcquisitionFailedException() {
        super(AuctionErrorCode.LOCK_ACQUISITION_FAILED);
    }

    public LockAcquisitionFailedException(String message) {
        super(AuctionErrorCode.LOCK_ACQUISITION_FAILED, message);
    }

    public LockAcquisitionFailedException(String message, Throwable cause) {
        super(AuctionErrorCode.LOCK_ACQUISITION_FAILED, message, cause);
    }
}
