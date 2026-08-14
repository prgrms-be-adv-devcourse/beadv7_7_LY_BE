package site.productservice.domain.search;

/**
 * 상품 검색 저장소. 지금은 DB LIKE 검색이지만, 파이널에 검색엔진(Elasticsearch)으로 갈아끼울 때
 * 이 인터페이스 뒤의 구현체만 바꾸면 되도록 계약을 특정 기술과 무관하게 유지한다.
 */
public interface ProductSearchRepository {

    /**
     * 검색어로 활성 상품을 찾는다. 토큰마다 제목·제목 별칭·아티스트명·아티스트 별칭 중 어디든 부분일치하면
     * 통과하고, 모든 토큰이 통과해야 결과에 잡힌다 — 단어 순서가 달라도 찾도록.
     * 정렬은 잘 맞는 순서: 카탈로그 번호 앞부분 일치 > 제목 정확 일치 > 아티스트(이름·별칭)
     * > 제목 앞부분 > 제목 부분 > 그 외. 같은 구간 안에서는 productId 오름차순으로 페이지가 흔들리지 않게
     * 고정한다. 아티스트가 제목 앞부분·부분보다 위인 이유: 이름을 검색한 사용자는 그 아티스트의 음반을
     * 원하지, 제목에 이름이 들어간 남의 앨범(트리뷰트 등)을 원하지 않는다.
     * 카탈로그 번호 매칭은 검색어에 숫자가 있을 때만 시도한다 — 일반 단어 검색이 번호와 우연히 겹쳐
     * 최상위를 차지하지 않도록.
     */
    ProductSearchPage searchActiveByKeyword(SearchKeyword keyword, int page, int size);

    /**
     * 카탈로그 브라우징. 활성 상품 전체를 최신 등록순(id 내림차순)으로 한 페이지 돌려준다.
     * 검색어로 걸러내는 건 searchActiveByKeyword 쪽 일이고, 여기는 조건 없이 훑는 용도다.
     */
    ProductSearchPage findActivePage(int page, int size);
}
