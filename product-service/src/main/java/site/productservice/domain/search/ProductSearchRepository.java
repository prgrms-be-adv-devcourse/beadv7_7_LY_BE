package site.productservice.domain.search;

/**
 * 상품 검색 저장소. 지금은 DB LIKE 검색이지만, 파이널에 검색엔진(Elasticsearch)으로 갈아끼울 때
 * 이 인터페이스 뒤의 구현체만 바꾸면 되도록 계약을 특정 기술과 무관하게 유지한다.
 */
public interface ProductSearchRepository {

    /**
     * 정규화된 검색어로 활성 상품을 찾는다. 제목·제목 별칭·아티스트명·아티스트 별칭 어디에 부분일치해도 잡힌다.
     * 정렬은 productId 오름차순 고정 — LIKE 검색엔 "더 잘 맞는 순서" 개념이 없어 페이지가 안 흔들리는 순서만 보장한다.
     */
    ProductSearchPage searchActiveByKeyword(String normalizedKeyword, int page, int size);

    /**
     * 카탈로그 브라우징. 활성 상품 전체를 최신 등록순(id 내림차순)으로 한 페이지 돌려준다.
     * 검색어로 걸러내는 건 searchActiveByKeyword 쪽 일이고, 여기는 조건 없이 훑는 용도다.
     */
    ProductSearchPage findActivePage(int page, int size);
}
