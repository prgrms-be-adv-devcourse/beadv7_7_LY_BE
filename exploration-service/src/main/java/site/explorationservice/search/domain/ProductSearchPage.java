package site.explorationservice.search.domain;

import java.util.List;

/**
 * 한 페이지 조회 결과와 조건에 맞는 전체 건수, 그리고 검색 엔진이 질의를 처리한 시간.
 * <p>
 * 전체 건수는 Elasticsearch가 기본적으로 10,000에서 세기를 멈추므로 질의에서 전수 집계를 켜서 받는다.
 * 프론트가 마지막 페이지 여부를 이 값으로 판단하기 때문에 상한이 걸리면 페이지네이션이 어긋난다.
 * <p>
 * 처리 시간은 검색 엔진이 응답에 실어 보내는 값이다. 서버가 요청을 받아 응답을 만들기까지 걸린 시간과 따로
 * 남겨야, 느려졌을 때 검색 엔진 쪽인지 애플리케이션 쪽인지 가를 수 있다. 조회를 건너뛴 경우에는 0이다.
 */
public record ProductSearchPage(List<ProductSearchHit> content, long totalElements, long engineMillis) {

    public static ProductSearchPage empty() {
        return new ProductSearchPage(List.of(), 0L, 0L);
    }
}
