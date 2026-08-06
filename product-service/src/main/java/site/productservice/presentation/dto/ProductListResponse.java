package site.productservice.presentation.dto;

import java.util.List;
import site.productservice.application.dto.ProductListResult;

/** 카탈로그 목록 응답. lastTradedPrice는 거래 이력 없으면, openAuctionCount는 경매 조회 실패면 null. */
public record ProductListResponse(List<Card> content, int page, int size, long totalElements, boolean hasNext) {

    public static ProductListResponse from(ProductListResult result) {
        List<Card> cards = result.content().stream()
                .map(item -> new Card(item.productId(), item.title(), item.artistName(), item.coverImageUrl(),
                        item.releaseYear(), item.pressType().name(), item.country(), item.lastTradedPrice(),
                        item.openAuctionCount()))
                .toList();
        return new ProductListResponse(cards, result.page(), result.size(), result.totalElements(),
                result.hasNext());
    }

    public record Card(Long productId, String title, String artistName, String coverImageUrl, int releaseYear,
            String pressType, String country, Long lastTradedPrice, Long openAuctionCount) {
    }
}
