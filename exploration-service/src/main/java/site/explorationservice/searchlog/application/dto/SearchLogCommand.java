package site.explorationservice.searchlog.application.dto;

/**
 * 검색 기록을 남겨 달라는 요청.
 * <p>
 * 검색 쪽이 기록 타입을 직접 만들지 않고 이 값을 넘긴다. 기록 타입에 필드가 늘거나 시각을 채우는 방식이
 * 바뀌어도 검색 쪽 코드가 따라 흔들리지 않는다.
 */
public record SearchLogCommand(String searchId, String keyword, String normalizedKeyword, String searchBy,
        int page, int size, long resultCount, long engineMillis, long elapsedMillis) {
}
