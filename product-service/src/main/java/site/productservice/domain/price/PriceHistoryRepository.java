package site.productservice.domain.price;

import java.util.List;
import java.util.Optional;

/**
 * 시세 기록 저장소 (도메인 인터페이스). 구현체는 infrastructure의 PriceHistoryRepositoryImpl.
 */
public interface PriceHistoryRepository {

    PriceHistory save(PriceHistory priceHistory);

    Optional<PriceHistory> findByAuctionId(Long auctionId);

    boolean existsByAuctionId(Long auctionId);

    /**
     * 한 상품의 최근 거래를 최신순(낙찰시각 내림차순, 같으면 id 내림차순)으로 최대 limit건 조회한다.
     * 집계에서 빼기로 표시된 거래(outlier)는 제외한다.
     * 시세 요약과 추이 API가 둘 다 이 조회 하나를 쓴다 — 두 화면이 항상 같은 데이터를 보게 하기 위해서다.
     */
    List<PriceHistory> findRecentTrades(Long productId, int limit);

    /**
     * 여러 상품의 가장 최근 거래를 상품당 1건씩 조회한다. 최신 기준은 findRecentTrades와 동일
     * (낙찰시각 내림차순, 같으면 id 내림차순)이고, 집계에서 빼기로 표시된 거래(outlier)는 제외한다.
     * 거래가 없는 상품은 결과에 나타나지 않는다.
     */
    List<PriceHistory> findLatestTrades(List<Long> productIds);
}
