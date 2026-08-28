package site.explorationservice.searchlog.domain;

/**
 * 기록을 저장하는 창구.
 * <p>
 * 검색과 클릭을 나눠 받는다. 둘은 쌓이는 빈도가 다르고 저장되는 인덱스도 달라서, 한 메서드로 합치면 부르는
 * 쪽이 어느 쪽인지 표시해 줘야 한다.
 */
public interface SearchLogRepository {

    void saveSearchLog(SearchLog searchLog);

    void saveClickLog(SearchClickLog clickLog);
}
