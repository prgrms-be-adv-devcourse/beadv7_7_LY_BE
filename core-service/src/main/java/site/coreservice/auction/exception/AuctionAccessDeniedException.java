package site.coreservice.auction.exception;

import site.common.exception.BusinessException;

public class AuctionAccessDeniedException extends BusinessException {

    public AuctionAccessDeniedException() {
        super(AuctionErrorCode.AUCTION_ACCESS_DENIED);
    }
}
