package site.coreservice.product.application.dto;

/**
 * 상품 목록 조회 조건. 지금은 페이징뿐이지만, 나중에 검색어·장르 필터가 붙을 자리를 만들어 두려고
 * 레코드로 감싼다 — 파라미터가 늘어도 호출부 시그니처가 흔들리지 않는다.
 * page·size 보정은 서비스가 한다.
 */
public record ProductListQuery(int page, int size) {
}
