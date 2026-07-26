package site.coreservice.auction.infrastructure;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.coreservice.auction.domain.AuctionStatus;

import java.util.List;

public interface AuctionSearchViewJpaRepository extends JpaRepository<AuctionSearchView, Long> {

    @Query("""
            SELECT v FROM AuctionSearchView v
            WHERE (:genre IS NULL OR v.genre = :genre)
              AND (:pressType IS NULL OR v.pressType = :pressType)
              AND (:status IS NULL OR v.status = :status)
            """)
    List<AuctionSearchView> search(@Param("genre") String genre, @Param("pressType") String pressType, @Param("status") AuctionStatus status, Pageable pageable);

    @Query("""
            SELECT COUNT(v) FROM AuctionSearchView v
            WHERE (:genre IS NULL OR v.genre = :genre)
              AND (:pressType IS NULL OR v.pressType = :pressType)
              AND (:status IS NULL OR v.status = :status)
            """)
    long countSearch(@Param("genre") String genre, @Param("pressType") String pressType, @Param("status") AuctionStatus status);

}
