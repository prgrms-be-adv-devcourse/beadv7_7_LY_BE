package site.explorationservice.search.domain;

import java.util.Arrays;
import site.explorationservice.search.exception.UnsupportedSearchTargetException;

/**
 * 무엇을 대상으로 찾을지 사용자가 지정하는 갈래.
 * <p>
 * 검색어가 번호인지 시스템이 추측하지 않기 위해 둔다. 숫자가 들어갔는지로 가르면 일반 단어로 시작하는 번호에
 * 걸려, 그 단어를 친 사람에게 엉뚱한 상품이 최상위로 올라온다.
 */
public enum SearchTarget {

    NAME,
    CATALOG;

    /**
     * 값을 안 주면 이름 검색으로 본다 — 이 파라미터가 생기기 전의 호출이 그대로 동작해야 하기 때문이다.
     * 모르는 값은 이름 검색으로 넘기지 않고 거절한다. 넘기면 오타를 낸 쪽이 다른 갈래의 결과를 받고도 모른다.
     */
    public static SearchTarget from(final String raw) {
        if (raw == null || raw.isBlank()) {
            return NAME;
        }
        final String requested = raw.trim();
        return Arrays.stream(values())
                // 이름을 대소문자 구분 없이 맞춘다. 자바 상수는 대문자로, 쿼리 파라미터는 소문자로 쓰는 관례라
                // 어딘가에서 한 번은 맞춰줘야 한다
                .filter(target -> target.name().equalsIgnoreCase(requested))
                .findFirst()
                .orElseThrow(UnsupportedSearchTargetException::new);
    }
}
