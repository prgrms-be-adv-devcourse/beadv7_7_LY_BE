package site.coreservice.auction.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AuctionRepository {

    Auction save(Auction auction);

    Optional<Auction> findById(Long id);

    List<Auction> findAllByIds(List<Long> ids);

    List<Auction> findAllScheduledToStart(LocalDateTime threshold);

    Map<Long, Long> countRunningByProductIds(List<Long> productIds);

    List<Auction> findAllRunningToEnd(LocalDateTime threshold);

    Page<Auction> findBySellerId(Long sellerId, Pageable pageable);
}
