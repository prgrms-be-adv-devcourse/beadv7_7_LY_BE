package site.coreservice.auction.infrastructure;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import site.coreservice.auction.domain.Bid;
import site.coreservice.auction.domain.BidOutcome;

import java.util.List;
import java.util.Optional;

public interface BidJpaRepository extends JpaRepository<Bid,Long> {
    Optional<Bid> findByAuctionIdAndOutcome(Long auctionId, BidOutcome outcome);

    List<Bid> findByAuctionIdOrderByPlacedAtDesc(Long auctionId, Pageable pageable);

    long countByAuctionId(Long auctionId);

}
