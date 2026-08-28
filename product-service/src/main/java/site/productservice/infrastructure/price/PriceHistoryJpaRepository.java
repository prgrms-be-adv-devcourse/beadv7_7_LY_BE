package site.productservice.infrastructure.price;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.productservice.domain.price.PriceHistory;

public interface PriceHistoryJpaRepository extends JpaRepository<PriceHistory, Long> {

    Optional<PriceHistory> findByAuctionId(Long auctionId);

    boolean existsByAuctionId(Long auctionId);

    List<PriceHistory> findByProductIdAndOutlierFalseOrderByTradedAtDescIdDesc(Long productId, Limit limit);

    List<PriceHistory> findByOutlierFalseOrderByTradedAtDescIdDesc(Limit limit);

    // 상품당 "낙찰시각 내림차순, 같으면 id 내림차순" 기준 1위만 남긴다 — 더 최신인 행이 존재하면 탈락
    @Query("""
            select ph from PriceHistory ph
            where ph.outlier = false
              and ph.productId in :productIds
              and not exists (select 1 from PriceHistory ph2
                              where ph2.productId = ph.productId
                                and ph2.outlier = false
                                and (ph2.tradedAt > ph.tradedAt
                                     or (ph2.tradedAt = ph.tradedAt and ph2.id > ph.id)))
            """)
    List<PriceHistory> findLatestTradesByProductIds(@Param("productIds") List<Long> productIds);
}
