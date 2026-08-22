package site.auctionservice.exception;

public class WalletBusinessException extends AuctionException {
    public WalletBusinessException(AuctionErrorCode code) {
        super(code);
    }

    public WalletBusinessException(AuctionErrorCode code, Throwable cause) {
        super(code, cause);
    }
}
