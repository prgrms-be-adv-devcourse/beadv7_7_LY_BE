package site.explorationservice.search.domain;

public interface ProductSearchRepository {

    /**
     * 살아 있는 상품만 대상으로 검색한다.
     * <p>
     * page는 0부터, size는 이미 보정된 값이 들어온다고 전제한다 — 보정은 응용 서비스의 몫이다.
     */
    ProductSearchPage search(SearchKeyword keyword, int page, int size);

    /**
     * 카탈로그 번호로만 대조한다. 검색어가 번호인지 추측하지 않고 호출한 쪽이 갈래를 정한다.
     * <p>
     * page·size 전제는 위 {@link #search}와 같다.
     */
    ProductSearchPage searchByCatalogNumber(SearchKeyword keyword, int page, int size);
}
