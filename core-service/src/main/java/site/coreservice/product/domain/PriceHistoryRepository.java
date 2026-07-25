package site.coreservice.product.domain;

import java.util.Optional;

/**
 * 시세 기록 저장소 (도메인 인터페이스). 구현체는 infrastructure의 PriceHistoryRepositoryImpl.
 */
public interface PriceHistoryRepository {

    PriceHistory save(PriceHistory priceHistory);

    Optional<PriceHistory> findByAuctionId(Long auctionId);

    boolean existsByAuctionId(Long auctionId);
}
