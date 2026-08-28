package site.productservice.application.dto.price;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import site.productservice.domain.Artist;
import site.productservice.domain.Product;
import site.productservice.domain.price.MediaCondition;
import site.productservice.domain.price.PriceHistory;

/**
 * 전역 최근 낙찰 결과. 거래 기록에 상품 표시 정보(제목·아티스트·커버)를 붙인다 —
 * 표시할 상품이 없는 거래(비활성·삭제)는 목록에서 제외한다.
 */
public record RecentTradesResult(List<RecentTrade> trades) {

    public static RecentTradesResult of(List<PriceHistory> priceHistories, Map<Long, Product> productsById,
            Map<Long, Artist> artistsById, int limit) {
        List<RecentTrade> trades = priceHistories.stream()
                .filter(history -> productsById.containsKey(history.getProductId()))
                .limit(limit)
                .map(history -> RecentTrade.of(history, productsById.get(history.getProductId()), artistsById))
                .toList();
        return new RecentTradesResult(trades);
    }

    public record RecentTrade(Long productId, String title, String artistName, String coverImageUrl,
            MediaCondition condition, long price, LocalDateTime tradedAt) {

        private static RecentTrade of(PriceHistory history, Product product, Map<Long, Artist> artistsById) {
            Artist artist = artistsById.get(product.getArtistId());
            return new RecentTrade(product.getId(), product.getTitle(),
                    artist == null ? null : artist.getName(), product.getCoverImage(),
                    history.getMediaCondition(), history.getFinalPrice(), history.getTradedAt());
        }
    }
}
