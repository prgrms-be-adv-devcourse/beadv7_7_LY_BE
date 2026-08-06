package site.coreservice.product.application.dto.search;

import java.util.List;
import site.coreservice.product.domain.search.ProductSearchHit;
import site.coreservice.product.domain.search.ProductSearchPage;

/** 상품 검색 결과(명세 1-1). page·size는 보정된 값이다. */
public record ProductSearchResult(List<ProductSearchHit> content, int page, int size, long totalElements,
        boolean hasNext) {

    public static ProductSearchResult of(ProductSearchPage searchPage, int page, int size) {
        boolean hasNext = (page + 1L) * size < searchPage.totalElements();
        return new ProductSearchResult(searchPage.content(), page, size, searchPage.totalElements(), hasNext);
    }

    public static ProductSearchResult empty(int page, int size) {
        return new ProductSearchResult(List.of(), page, size, 0L, false);
    }
}
