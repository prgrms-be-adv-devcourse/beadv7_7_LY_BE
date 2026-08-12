package site.productservice.domain.search;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import site.productservice.domain.TextNormalizer;

/**
 * 검색어를 저장값과 같은 규칙으로 손질해 두 가지 형태로 제공한다.
 * - 토큰: 공백 단위로 나눠 각각 표기 통일한 것 — 단어 순서가 달라도 찾도록 조건을 만들 때 쓴다
 * - 전체: 통째로 표기 통일한 것 — 카탈로그 번호처럼 붙여 쓴 값과 앞부분 일치를 볼 때 쓴다
 */
public final class SearchKeyword {

    private final List<String> tokens;
    private final String whole;

    private SearchKeyword(List<String> tokens, String whole) {
        this.tokens = tokens;
        this.whole = whole;
    }

    public static SearchKeyword from(String raw) {
        if (raw == null || raw.isBlank()) {
            return new SearchKeyword(List.of(), null);
        }
        List<String> tokens = Arrays.stream(raw.trim().split("\\s+"))
                .map(TextNormalizer::normalize)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return new SearchKeyword(tokens, TextNormalizer.normalize(raw));
    }

    public boolean isEmpty() {
        return tokens.isEmpty();
    }

    public List<String> getTokens() {
        return tokens;
    }

    public String getWhole() {
        return whole;
    }
}
