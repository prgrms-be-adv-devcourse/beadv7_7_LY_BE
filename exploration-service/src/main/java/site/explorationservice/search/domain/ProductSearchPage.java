package site.explorationservice.search.domain;

import java.util.List;

/**
 * 한 페이지 조회 결과와 조건에 맞는 전체 건수.
 * <p>
 * 전체 건수는 Elasticsearch가 기본적으로 10,000에서 세기를 멈추므로 질의에서 전수 집계를 켜서 받는다.
 * 프론트가 마지막 페이지 여부를 이 값으로 판단하기 때문에 상한이 걸리면 페이지네이션이 어긋난다.
 */
public record ProductSearchPage(List<ProductSearchHit> content, long totalElements) {

    public static ProductSearchPage empty() {
        return new ProductSearchPage(List.of(), 0L);
    }
}
