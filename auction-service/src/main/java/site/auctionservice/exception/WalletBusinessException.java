package site.auctionservice.exception;

public class WalletBusinessException extends AuctionException {
    public WalletBusinessException(AuctionErrorCode code) {
        super(code);
    }
}
