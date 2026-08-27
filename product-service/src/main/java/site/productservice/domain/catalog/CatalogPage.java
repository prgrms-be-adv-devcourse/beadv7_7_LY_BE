package site.productservice.domain.catalog;

import java.util.List;

/**
 * 카탈로그 목록 한 페이지.
 * hasNext는 저장소가 판단한다 — 한 건 더 읽어봐야 알 수 있고, 그 사정을 호출자가 알 필요는 없다.
 * totalElements는 별도 건수 조회 결과라 화면 표시용이고, 다음 페이지 유무와는 계산 경로가 다르다.
 */
public record CatalogPage(List<CatalogItem> items, long totalElements, boolean hasNext) {
}
