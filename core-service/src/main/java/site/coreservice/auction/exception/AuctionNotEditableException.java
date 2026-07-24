package site.coreservice.auction.exception;

import site.common.exception.BusinessException;

public class AuctionNotEditableException extends BusinessException {

    public AuctionNotEditableException() {
        super(AuctionErrorCode.AUCTION_NOT_EDITABLE);
    }
}
