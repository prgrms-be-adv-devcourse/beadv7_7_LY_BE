package site.auctionservice.application.port;

public interface BidOutbidMarkPort {

    void markOutbid(Long auctionId, Long previousBidderId);
}
