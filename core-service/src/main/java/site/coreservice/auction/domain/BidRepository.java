package site.coreservice.auction.domain;

import java.util.List;
import java.util.Optional;

public interface BidRepository {
    Bid save(Bid bid);

    Optional<Bid> findById(Long id);

    Optional<Bid> findActiveBid(Long auctionId);

    List<Bid> findRecentByAuctionId(Long auctionId, int limit);

    long countByAuctionId(Long auctionId);
}
