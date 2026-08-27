package site.productservice.domain.catalog;

/**
 * 카탈로그 브라우징 저장소. 활성 상품 전체를 최신 등록순으로 한 페이지 돌려준다.
 * 검색어로 걸러내는 것은 검색의 일이고 여기는 조건 없이 훑는 용도다.
 */
public interface CatalogRepository {

    CatalogPage findActivePage(int page, int size);
}
