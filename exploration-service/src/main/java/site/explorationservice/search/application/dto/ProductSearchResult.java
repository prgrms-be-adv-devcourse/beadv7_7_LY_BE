package site.explorationservice.search.application.dto;

import java.util.List;
import site.explorationservice.search.domain.ProductSearchHit;
import site.explorationservice.search.domain.ProductSearchPage;

/** page·size는 보정이 끝난 값이다. */
public record ProductSearchResult(List<ProductSearchHit> content, int page, int size, long totalElements,
    boolean hasNext) {

    public static ProductSearchResult of(final ProductSearchPage page, final int pageNumber, final int size) {
        // long 으로 계산한다. int 로 두면 페이지 번호가 커질 때 곱셈에서 넘쳐 hasNext 가 뒤집힌다.
        final boolean hasNext = (pageNumber + 1L) * size < page.totalElements();
        return new ProductSearchResult(page.content(), pageNumber, size, page.totalElements(), hasNext);
    }

    public static ProductSearchResult empty(final int pageNumber, final int size) {
        return new ProductSearchResult(List.of(), pageNumber, size, 0L, false);
    }
}
