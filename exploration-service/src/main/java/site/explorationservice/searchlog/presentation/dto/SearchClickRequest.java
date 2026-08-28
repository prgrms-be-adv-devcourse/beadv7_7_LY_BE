package site.explorationservice.searchlog.presentation.dto;

import site.explorationservice.searchlog.domain.SearchClickLog;

/**
 * 검색 결과에서 무엇을 눌렀는지 알리는 요청.
 * <p>
 * 값을 상자 타입으로 받는 이유는 본문에 항목이 빠졌을 때를 구분하기 위해서다. 기본형으로 받으면 순위를 안
 * 보낸 요청이 0으로 채워져 들어와, 값을 빠뜨린 것과 0을 보낸 것이 같아진다.
 */
public record SearchClickRequest(String searchId, Long productId, Integer rank) {

    public SearchClickLog toSearchClickLog() {
        return SearchClickLog.of(searchId, productId, rank);
    }
}
