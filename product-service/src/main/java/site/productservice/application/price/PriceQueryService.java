package site.productservice.application.price;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.productservice.application.dto.price.PriceSummaryResult;
import site.productservice.application.dto.price.PriceTradesResult;
import site.productservice.application.dto.price.RecentTradesResult;
import site.productservice.domain.price.ConditionPriceStat;
import site.productservice.domain.price.PriceHistory;
import site.productservice.domain.price.PriceHistoryRepository;
import site.productservice.domain.Artist;
import site.productservice.domain.ArtistRepository;
import site.productservice.domain.Product;
import site.productservice.domain.ProductRepository;
import site.productservice.exception.ProductNotFoundException;

/**
 * 시세 조회 서비스 (요약 3-1 · 추이 3-2). 두 API가 같은 창(최근 100건, 이상치 제외)을 읽는다 —
 * 요약 표본 수 합계와 차트 점 개수가 어긋나지 않게 하기 위한 스펙 결정 ④.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PriceQueryService {

    /** 시세 창 크기. 요약·추이 공용 — 한쪽만 바꾸면 두 화면이 어긋나므로 상수 하나로 묶는다. */
    static final int RECENT_TRADES_LIMIT = 100;

    /** 전역 최근 낙찰 조회 한도 — 홈 표시용이라 큰 페이지가 필요 없다. */
    static final int GLOBAL_RECENT_MAX_SIZE = 20;

    /** 비활성 상품 거래를 걸러도 요청 개수를 채울 수 있게 여유를 두고 읽는 배수. */
    private static final int GLOBAL_RECENT_FETCH_FACTOR = 2;

    private final ProductRepository productRepository;
    private final ArtistRepository artistRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    public PriceSummaryResult getPriceSummary(Long productId) {
        validateActiveProduct(productId);
        List<PriceHistory> trades = priceHistoryRepository.findRecentTrades(productId, RECENT_TRADES_LIMIT);
        return PriceSummaryResult.of(productId, ConditionPriceStat.listFrom(trades));
    }

    public PriceTradesResult getPriceTrades(Long productId) {
        validateActiveProduct(productId);
        List<PriceHistory> trades = priceHistoryRepository.findRecentTrades(productId, RECENT_TRADES_LIMIT);
        return PriceTradesResult.of(productId, trades);
    }

    /**
     * 전체 상품을 통틀어 최근 낙찰 size건을 최신순으로 조회한다 (홈 최근 낙찰 목록용).
     * 표시할 상품이 없는 거래(비활성·삭제)는 건너뛴다 — 눌러도 갈 곳이 없는 행을 만들지 않기 위해서다.
     */
    public RecentTradesResult getRecentTrades(int size) {
        int limit = Math.min(Math.max(size, 1), GLOBAL_RECENT_MAX_SIZE);
        List<PriceHistory> trades = priceHistoryRepository.findRecent(limit * GLOBAL_RECENT_FETCH_FACTOR);
        Map<Long, Product> productsById = findActiveProducts(trades);
        Map<Long, Artist> artistsById = findArtists(productsById.values());
        return RecentTradesResult.of(trades, productsById, artistsById, limit);
    }

    private Map<Long, Product> findActiveProducts(List<PriceHistory> trades) {
        List<Long> productIds = trades.stream()
                .map(PriceHistory::getProductId)
                .distinct()
                .toList();
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return productRepository.findAllByIds(productIds).stream()
                .filter(Product::isActive)
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private Map<Long, Artist> findArtists(Collection<Product> products) {
        List<Long> artistIds = products.stream()
                .map(Product::getArtistId)
                .distinct()
                .toList();
        if (artistIds.isEmpty()) {
            return Map.of();
        }
        return artistRepository.findAllByIds(artistIds).stream()
                .collect(Collectors.toMap(Artist::getId, Function.identity()));
    }

    /** 비활성 상품은 사용자에겐 없는 상품이므로 404로 취급한다 — 카탈로그 상세와 같은 기준. */
    private void validateActiveProduct(Long productId) {
        productRepository.findById(productId)
                .filter(Product::isActive)
                .orElseThrow(ProductNotFoundException::new);
    }
}
