package site.explorationservice.search.application.dto;

import java.util.List;
import site.explorationservice.search.domain.ProductSearchHit;
import site.explorationservice.search.domain.ProductSearchPage;

/**
 * page·size는 보정이 끝난 값이다.
 * <p>
 * searchId는 이 검색을 가리키는 값이다. 응답에 실려 나가고, 프론트가 결과를 눌렀을 때 그대로 돌려보내면
 * 클릭 기록을 이 검색에 이어 붙일 수 있다.
 */
public record ProductSearchResult(List<ProductSearchHit> content, int page, int size, long totalElements,
    boolean hasNext, String searchId) {

    public static ProductSearchResult of(final ProductSearchPage page, final int pageNumber, final int size,
            final String searchId) {
        // long 으로 계산한다. int 로 두면 페이지 번호가 커질 때 곱셈에서 넘쳐 hasNext 가 뒤집힌다.
        final boolean hasNext = (pageNumber + 1L) * size < page.totalElements();
        return new ProductSearchResult(page.content(), pageNumber, size, page.totalElements(), hasNext, searchId);
    }
}
