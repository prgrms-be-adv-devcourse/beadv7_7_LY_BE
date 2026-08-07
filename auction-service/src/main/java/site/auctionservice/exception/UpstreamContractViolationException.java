package site.auctionservice.exception;

import site.common.exception.BusinessException;

public class UpstreamContractViolationException extends BusinessException {

    public UpstreamContractViolationException() {
        super(AuctionErrorCode.UPSTREAM_CONTRACT_VIOLATION);
    }

    public UpstreamContractViolationException(String message) {
        super(AuctionErrorCode.UPSTREAM_CONTRACT_VIOLATION, message);
    }
}