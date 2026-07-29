package site.coreservice.auction.infrastructure;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuctionSearchViewJpaRepository extends JpaRepository<AuctionSearchView, Long> {

    // status 컬럼 없이 startAt/endAt/bidCount만으로 상태별 조회를 나눈다.
    // CANCELED는 삭제되는 행이라 여기 없음.

    @Query("""
        SELECT v FROM AuctionSearchView v
        WHERE (:genre IS NULL OR v.genre = :genre)
          AND (:pressType IS NULL OR v.pressType = :pressType)
        """)
    List<AuctionSearchView> searchAll(@Param("genre") String genre,
        @Param("pressType") String pressType, Pageable pageable);

    @Query("""
        SELECT COUNT(v) FROM AuctionSearchView v
        WHERE (:genre IS NULL OR v.genre = :genre)
          AND (:pressType IS NULL OR v.pressType = :pressType)
        """)
    long countSearchAll(@Param("genre") String genre, @Param("pressType") String pressType);

    @Query("""
        SELECT v FROM AuctionSearchView v
        WHERE (:genre IS NULL OR v.genre = :genre)
          AND (:pressType IS NULL OR v.pressType = :pressType)
          AND v.startAt > :now
        """)
    List<AuctionSearchView> searchScheduled(@Param("genre") String genre,
        @Param("pressType") String pressType,
        @Param("now") LocalDateTime now, Pageable pageable);

    @Query("""
        SELECT COUNT(v) FROM AuctionSearchView v
        WHERE (:genre IS NULL OR v.genre = :genre)
          AND (:pressType IS NULL OR v.pressType = :pressType)
          AND v.startAt > :now
        """)
    long countSearchScheduled(@Param("genre") String genre, @Param("pressType") String pressType,
        @Param("now") LocalDateTime now);

    @Query("""
        SELECT v FROM AuctionSearchView v
        WHERE (:genre IS NULL OR v.genre = :genre)
          AND (:pressType IS NULL OR v.pressType = :pressType)
          AND v.startAt <= :now AND v.endAt > :now
        """)
    List<AuctionSearchView> searchRunning(@Param("genre") String genre,
        @Param("pressType") String pressType,
        @Param("now") LocalDateTime now, Pageable pageable);

    @Query("""
        SELECT COUNT(v) FROM AuctionSearchView v
        WHERE (:genre IS NULL OR v.genre = :genre)
          AND (:pressType IS NULL OR v.pressType = :pressType)
          AND v.startAt <= :now AND v.endAt > :now
        """)
    long countSearchRunning(@Param("genre") String genre, @Param("pressType") String pressType,
        @Param("now") LocalDateTime now);

    @Query("""
        SELECT v FROM AuctionSearchView v
        WHERE (:genre IS NULL OR v.genre = :genre)
          AND (:pressType IS NULL OR v.pressType = :pressType)
          AND v.endAt <= :now AND v.bidCount > 0
        """)
    List<AuctionSearchView> searchEndedWon(@Param("genre") String genre,
        @Param("pressType") String pressType,
        @Param("now") LocalDateTime now, Pageable pageable);

    @Query("""
        SELECT COUNT(v) FROM AuctionSearchView v
        WHERE (:genre IS NULL OR v.genre = :genre)
          AND (:pressType IS NULL OR v.pressType = :pressType)
          AND v.endAt <= :now AND v.bidCount > 0
        """)
    long countSearchEndedWon(@Param("genre") String genre, @Param("pressType") String pressType,
        @Param("now") LocalDateTime now);

    @Query("""
        SELECT v FROM AuctionSearchView v
        WHERE (:genre IS NULL OR v.genre = :genre)
          AND (:pressType IS NULL OR v.pressType = :pressType)
          AND v.endAt <= :now AND v.bidCount = 0
        """)
    List<AuctionSearchView> searchEndedFailed(@Param("genre") String genre,
        @Param("pressType") String pressType,
        @Param("now") LocalDateTime now, Pageable pageable);

    @Query("""
        SELECT COUNT(v) FROM AuctionSearchView v
        WHERE (:genre IS NULL OR v.genre = :genre)
          AND (:pressType IS NULL OR v.pressType = :pressType)
          AND v.endAt <= :now AND v.bidCount = 0
        """)
    long countSearchEndedFailed(@Param("genre") String genre, @Param("pressType") String pressType,
        @Param("now") LocalDateTime now);

}
