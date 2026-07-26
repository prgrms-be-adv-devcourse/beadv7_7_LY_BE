package site.coreservice.auction.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface BidRepository {
    Bid save(Bid bid);

    Optional<Bid> findById(Long id);

    Optional<Bid> findActiveBid(Long auctionId);

    Page<Bid> findLatestBidsByBidder(Long bidderId, Pageable pageable);

    Map<Long, Long> countGroupedByAuctionIds(List<Long> auctionIds);
}
