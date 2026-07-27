package site.coreservice.product.infrastructure;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import site.coreservice.product.domain.PriceHistory;

public interface PriceHistoryJpaRepository extends JpaRepository<PriceHistory, Long> {

    Optional<PriceHistory> findByAuctionId(Long auctionId);

    boolean existsByAuctionId(Long auctionId);

    List<PriceHistory> findByProductIdAndOutlierFalseOrderByTradedAtDescIdDesc(Long productId, Limit limit);
}
