package site.coreservice.auction.infrastructure;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.coreservice.auction.domain.Auction;
import site.coreservice.auction.domain.AuctionStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface AuctionJpaRepository extends JpaRepository<Auction, Long> {

    @Query("SELECT a FROM Auction a WHERE a.status = :status AND a.schedule.period.startAt <= :threshold")
    List<Auction> findAllByStatusAndStartAtLessThanEqual(@Param("status") AuctionStatus status,
        @Param("threshold") LocalDateTime threshold);

    @Query("SELECT a FROM Auction a WHERE a.status = :status AND a.schedule.period.endAt <= :threshold")
    List<Auction> findAllByStatusAndEndAtLessThanEqual(@Param("status") AuctionStatus status,
        @Param("threshold") LocalDateTime threshold);

    boolean existsByProductId(Long productId);

    @Query("SELECT a FROM Auction a WHERE a.sellerId = :sellerId ORDER BY a.createdAt DESC")
    List<Auction> findBySellerId(@Param("sellerId") Long sellerId, Pageable pageable);

    @Query("SELECT COUNT(a) FROM Auction a WHERE a.sellerId = :sellerId")
    long countBySellerId(@Param("sellerId") Long sellerId);
}
