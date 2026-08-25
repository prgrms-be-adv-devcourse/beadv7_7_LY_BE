package site.explorationservice.search.domain;

public interface ProductSearchRepository {

    /**
     * 살아 있는 상품만 대상으로 검색한다.
     * <p>
     * page는 0부터, size는 이미 보정된 값이 들어온다고 전제한다 — 보정은 응용 서비스의 몫이다.
     */
    ProductSearchPage search(SearchKeyword keyword, int page, int size);
}
