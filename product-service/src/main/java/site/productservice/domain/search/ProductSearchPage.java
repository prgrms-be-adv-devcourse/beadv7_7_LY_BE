package site.productservice.domain.search;

import java.util.List;

/** 검색 결과 한 페이지. hasNext 계산은 서비스 몫 (page·size는 호출자가 알고 있음). */
public record ProductSearchPage(List<ProductSearchHit> content, long totalElements) {
}
