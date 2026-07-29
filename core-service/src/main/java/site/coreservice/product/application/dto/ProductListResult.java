package site.coreservice.product.application.dto;

import java.util.List;
import site.coreservice.product.domain.PressType;

/** 상품 목록 결과. lastTradedPrice는 거래 이력 없으면, openAuctionCount는 경매 조회 실패면 null. */
public record ProductListResult(List<Item> content, int page, int size, long totalElements, boolean hasNext) {

    public static ProductListResult of(List<Item> content, int page, int size, long totalElements) {
        boolean hasNext = (page + 1L) * size < totalElements;
        return new ProductListResult(content, page, size, totalElements, hasNext);
    }

    public record Item(Long productId, String title, String artistName, String coverImageUrl, int releaseYear,
            PressType pressType, String country, Long lastTradedPrice, Long openAuctionCount) {
    }
}
