package site.explorationservice.search.domain;

import site.common.text.TextNormalizer;

/**
 * 검색어를 질의에 넣기 전 손질한다.
 * <p>
 * product-service의 같은 이름 값 객체와 달리 <b>표기 통일을 하지 않는다.</b> LIKE 검색은 저장값과 검색어를 같은
 * 규칙으로 다듬어야 했지만, Elasticsearch는 색인할 때와 검색할 때 같은 분석기를 태우므로 그 일이 이미 끝나 있다.
 * 여기서 대소문자나 악센트를 건드리면 오히려 분석기가 하는 일과 어긋난다.
 */
public final class SearchKeyword {

    private static final int MIN_LENGTH = 2;

    private final String value;
    private final String normalized;

    private SearchKeyword(final String value) {
        this.value = value;
        // TextNormalizer는 남는 글자가 없으면 null을 준다. 위 계층이 null을 검사하게 만들지 않으려고 여기서 흡수한다
        final String normalizedValue = TextNormalizer.normalize(value);
        this.normalized = normalizedValue == null ? "" : normalizedValue;
    }

    public static SearchKeyword from(final String raw) {
        if (raw == null || raw.isBlank()) {
            return new SearchKeyword("");
        }
        return new SearchKeyword(raw.trim().replaceAll("\\s+", " "));
    }

    /**
     * 한 글자 검색어는 거의 모든 문서에 걸려 결과가 의미를 잃는다. 잘못된 요청이 아니라 "결과 없음"으로 다룬다 —
     * 응답 형식이 상황에 따라 달라지면 프론트가 두 갈래로 분기해야 한다.
     */
    public boolean isTooShort() {
        return value.length() < MIN_LENGTH;
    }

    public String getValue() {
        return value;
    }

    /**
     * 번호 대조에만 쓰는, 표기를 통일한 값. 색인할 때와 같은 규칙이라 저장된 값과 글자 단위로 맞는다.
     * 이름 검색은 원문을 쓴다 — 그쪽 필드는 분석기를 타기 때문이다.
     */
    public String getNormalized() {
        return normalized;
    }

    /** 글자·숫자가 하나도 없는 검색어는 번호로 대조할 수 없다. */
    public boolean hasNormalized() {
        return !normalized.isEmpty();
    }
}
