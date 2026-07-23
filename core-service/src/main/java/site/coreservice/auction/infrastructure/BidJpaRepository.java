package site.coreservice.auction.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import site.coreservice.auction.domain.Bid;
import site.coreservice.auction.domain.BidOutcome;

import java.util.Optional;

public interface BidJpaRepository extends JpaRepository<Bid,Long> {
    Optional<Bid> findByAuctionIdAndOutcome(Long auctionId, BidOutcome outcome);

}
