package site.coreservice.auction.exception;

import site.common.exception.BusinessException;

public class AuctionNotFoundException extends BusinessException {

    public AuctionNotFoundException() {
        super(AuctionErrorCode.AUCTION_NOT_FOUND);
    }
}
