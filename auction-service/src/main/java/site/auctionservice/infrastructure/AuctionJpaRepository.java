package site.auctionservice.infrastructure;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.auctionservice.domain.Auction;
import site.auctionservice.domain.AuctionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuctionJpaRepository extends JpaRepository<Auction, Long> {

    /*  비관적 쓰기 락(SELECT ... FOR UPDATE)으로 조회. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Auction a WHERE a.id = :id")
    Optional<Auction> findByIdForUpdate(@Param("id") Long id);

    /* 이 커넥션(세션)의 InnoDB 락 대기 상한선을 짧게 줄인다. findByIdForUpdate() 직전에 매번 호출한다. */
    @Modifying
    @Query(value = "SET innodb_lock_wait_timeout = :seconds", nativeQuery = true)
    void setLockWaitTimeout(@Param("seconds") int seconds);

    @Query("SELECT a FROM Auction a WHERE a.status = :status AND a.schedule.period.startAt <= :threshold")
    List<Auction> findAllByStatusAndStartAtLessThanEqual(@Param("status") AuctionStatus status,
        @Param("threshold") LocalDateTime threshold);

    @Query("SELECT a FROM Auction a WHERE a.status = :status AND a.schedule.period.endAt <= :threshold")
    List<Auction> findAllByStatusAndEndAtLessThanEqual(@Param("status") AuctionStatus status,
        @Param("threshold") LocalDateTime threshold);

    boolean existsByProductId(Long productId);

    @Query("SELECT a.productId AS productId, COUNT(a) AS count FROM Auction a " +
        "WHERE a.productId IN :productIds AND a.status = :status GROUP BY a.productId")
    List<ProductAuctionCountRow> countByProductIdsAndStatus(@Param("productIds") List<Long> productIds,
        @Param("status") AuctionStatus status);

    interface ProductAuctionCountRow {
        Long getProductId();
        Long getCount();
    }

    @Query("SELECT a FROM Auction a WHERE a.sellerId = :sellerId ORDER BY a.createdAt DESC")
    List<Auction> findBySellerId(@Param("sellerId") Long sellerId, Pageable pageable);

    @Query("SELECT COUNT(a) FROM Auction a WHERE a.sellerId = :sellerId")
    long countBySellerId(@Param("sellerId") Long sellerId);
}
