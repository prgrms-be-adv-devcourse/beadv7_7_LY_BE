package site.auctionservice.application.port;

public interface BidReactionPort {

    void recordReactionIfApplicable(Long auctionId, Long bidderId);
}
