package site.coreservice.product.infrastructure;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;
import site.coreservice.product.domain.PriceHistory;
import site.coreservice.product.domain.PriceHistoryRepository;

@Repository
@RequiredArgsConstructor
public class PriceHistoryRepositoryImpl implements PriceHistoryRepository {

    private final PriceHistoryJpaRepository priceHistoryJpaRepository;

    @Override
    public PriceHistory save(PriceHistory priceHistory) {
        return priceHistoryJpaRepository.save(priceHistory);
    }

    @Override
    public Optional<PriceHistory> findByAuctionId(Long auctionId) {
        return priceHistoryJpaRepository.findByAuctionId(auctionId);
    }

    @Override
    public boolean existsByAuctionId(Long auctionId) {
        return priceHistoryJpaRepository.existsByAuctionId(auctionId);
    }

    @Override
    public List<PriceHistory> findRecentTrades(Long productId, int limit) {
        return priceHistoryJpaRepository
                .findByProductIdAndOutlierFalseOrderByTradedAtDescIdDesc(productId, Limit.of(limit));
    }
}
