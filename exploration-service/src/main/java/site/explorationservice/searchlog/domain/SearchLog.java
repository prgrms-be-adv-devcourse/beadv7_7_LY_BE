package site.explorationservice.searchlog.domain;

import java.time.Instant;

/**
 * 검색 한 번의 기록.
 * <p>
 * 검색어를 다듬은 값까지 함께 남기는 이유는, 같은 의도로 친 검색을 하나로 묶어 세기 위해서다. 원문만 남기면
 * 띄어쓰기나 기호가 다른 입력이 서로 다른 검색어로 집계된다.
 * <p>
 * 시간을 두 개 남긴다. {@code engineMillis}는 검색 엔진이 질의를 처리한 시간이고 {@code elapsedMillis}는
 * 서버가 요청을 받아 응답을 만들기까지 걸린 시간이다. 둘 다 크면 검색 엔진이 느린 것이고, 앞은 작은데 뒤만
 * 크면 애플리케이션 쪽 문제다.
 * <p>
 * 검색 도메인의 타입을 받지 않고 문자열로 받는다. 이 도메인이 검색 도메인의 내부 타입을 알면 두 기능이 서로의
 * 사정에 묶인다.
 * <p>
 * 시각은 시간대 정보가 붙는 타입으로 남긴다. 이 서비스의 다른 검색 문서도 같은 타입을 쓰고 있고,
 * 서버와 조회 도구의 시간대가 다를 때 값이 어긋나지 않는다.
 */
public record SearchLog(String searchId, String keyword, String normalizedKeyword, String searchBy,
    int page, int size, long resultCount, long engineMillis, long elapsedMillis, Instant searchedAt) {

    public static SearchLog of(final String searchId, final String keyword, final String normalizedKeyword,
            final String searchBy, final int page, final int size, final long resultCount,
            final long engineMillis, final long elapsedMillis) {
        return new SearchLog(searchId, keyword, normalizedKeyword, searchBy, page, size, resultCount,
                engineMillis, elapsedMillis, Instant.now());
    }
}
