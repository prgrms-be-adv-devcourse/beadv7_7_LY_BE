package site.coreservice.auction.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AuctionRepository {

    Auction save(Auction auction);

    Optional<Auction> findById(Long id);

    List<Auction> findAllScheduledToStart(LocalDateTime threshold);

    Map<Long, Long> countRunningByProductIds(List<Long> productIds);

    List<Auction> findAllRunningToEnd(LocalDateTime threshold);
}
