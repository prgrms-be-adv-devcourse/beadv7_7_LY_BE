package site.coreservice.product.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.coreservice.product.application.dto.PriceSummaryResult;
import site.coreservice.product.application.dto.PriceTradesResult;
import site.coreservice.product.domain.ConditionPriceStat;
import site.coreservice.product.domain.PriceHistory;
import site.coreservice.product.domain.PriceHistoryRepository;
import site.coreservice.product.domain.Product;
import site.coreservice.product.domain.ProductRepository;
import site.coreservice.product.exception.ProductNotFoundException;

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

    private final ProductRepository productRepository;
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

    /** 비활성 상품은 사용자에겐 없는 상품이므로 404로 취급한다 — 카탈로그 상세와 같은 기준. */
    private void validateActiveProduct(Long productId) {
        productRepository.findById(productId)
                .filter(Product::isActive)
                .orElseThrow(ProductNotFoundException::new);
    }
}
