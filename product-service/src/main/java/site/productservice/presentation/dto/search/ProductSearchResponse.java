package site.productservice.presentation.dto.search;

import java.util.List;
import site.productservice.application.dto.search.ProductSearchResult;

/** 검색 응답(명세 1-1). openAuctionCount·lastTradedPrice는 아직 없다 — 각각 경매 연동(D7)·시세(D5) 후 추가. */
public record ProductSearchResponse(List<Card> content, int page, int size, long totalElements, boolean hasNext) {

    public static ProductSearchResponse from(ProductSearchResult result) {
        List<Card> cards = result.content().stream()
                .map(hit -> new Card(hit.productId(), hit.title(), hit.artistName(), hit.coverImageUrl(),
                        hit.releaseYear(), hit.pressType().name()))
                .toList();
        return new ProductSearchResponse(cards, result.page(), result.size(), result.totalElements(),
                result.hasNext());
    }

    public record Card(Long productId, String title, String artistName, String coverImageUrl, int releaseYear,
            String pressType) {
    }
}
