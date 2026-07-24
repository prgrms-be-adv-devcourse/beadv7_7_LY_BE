package site.coreservice.auction.exception;

import site.common.exception.BusinessException;

public class AuctionSearchViewNotFoundException extends BusinessException {

    public AuctionSearchViewNotFoundException() {
        super(AuctionErrorCode.AUCTION_SEARCH_VIEW_NOT_FOUND);
    }
}
