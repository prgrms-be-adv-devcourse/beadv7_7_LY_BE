package site.coreservice.auction.infrastructure;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.coreservice.auction.domain.Bid;
import site.coreservice.auction.domain.BidOutcome;

import java.util.List;
import java.util.Optional;

public interface BidJpaRepository extends JpaRepository<Bid,Long> {
    Optional<Bid> findByAuctionIdAndOutcome(Long auctionId, BidOutcome outcome);

    List<Bid> findByAuctionIdOrderByPlacedAtDesc(Long auctionId, Pageable pageable);

    long countByAuctionId(Long auctionId);

    @Query("""
            SELECT b FROM Bid b
            WHERE b.bidderId = :bidderId
              AND b.placedAt = (
                  SELECT MAX(b2.placedAt) FROM Bid b2
                  WHERE b2.auctionId = b.auctionId AND b2.bidderId = :bidderId
              )
            ORDER BY b.placedAt DESC
            """)
    List<Bid> findLatestBidsByBidder(@Param("bidderId") Long bidderId, Pageable pageable);

    @Query("SELECT COUNT(DISTINCT b.auctionId) FROM Bid b WHERE b.bidderId = :bidderId")
    long countDistinctAuctionsByBidder(@Param("bidderId") Long bidderId);

}
