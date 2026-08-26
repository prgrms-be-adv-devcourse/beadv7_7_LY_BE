package site.explorationservice.search.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("검색어")
class SearchKeywordTest {

    @Test
    @DisplayName("앞뒤 공백을 제거한다")
    void 공백_제거() {
        // given
        final String raw = "  장기하  ";

        // when
        final SearchKeyword keyword = SearchKeyword.from(raw);

        // then
        assertThat(keyword.getValue()).isEqualTo("장기하");
    }

    @Test
    @DisplayName("단어 사이 연속 공백을 하나로 줄인다")
    void 연속_공백_축약() {
        // given
        final String raw = "장기하와    얼굴들";

        // when
        final SearchKeyword keyword = SearchKeyword.from(raw);

        // then
        // 공백이 여러 개면 ES 질의 문자열에 빈 토큰이 섞인다
        assertThat(keyword.getValue()).isEqualTo("장기하와 얼굴들");
    }

    @Test
    @DisplayName("한 글자 검색어는 너무 짧은 것으로 본다")
    void 한_글자는_짧음() {
        // given
        final SearchKeyword keyword = SearchKeyword.from("a");

        // when & then
        assertThat(keyword.isTooShort()).isTrue();
    }

    @Test
    @DisplayName("두 글자 검색어는 짧지 않다")
    void 두_글자는_통과() {
        // given
        final SearchKeyword keyword = SearchKeyword.from("aa");

        // when & then
        assertThat(keyword.isTooShort()).isFalse();
    }

    @Test
    @DisplayName("공백만 있는 검색어는 빈 값이 되고 너무 짧은 것으로 본다")
    void 공백만_있으면_짧음() {
        // given
        final SearchKeyword keyword = SearchKeyword.from("   ");

        // when & then
        assertThat(keyword.getValue()).isEmpty();
        assertThat(keyword.isTooShort()).isTrue();
    }

    @Test
    @DisplayName("null 검색어도 빈 값으로 다룬다")
    void null_허용() {
        // given
        final SearchKeyword keyword = SearchKeyword.from(null);

        // when & then
        // null 검증은 서비스가 먼저 하지만, 값 객체가 터지면 원인 파악이 어려워진다
        assertThat(keyword.getValue()).isEmpty();
        assertThat(keyword.isTooShort()).isTrue();
    }

    @Test
    @DisplayName("표기가_달라도_같은_번호는_같은_정규화_값이_된다")
    void 번호_표기_통일() {
        // given
        final String hyphen = "BLP-1567";
        final String spaced = "blp 1567";
        final String joined = "blp1567";

        // when & then
        // 색인할 때와 같은 규칙을 타야 저장된 값과 맞는다
        assertThat(SearchKeyword.from(hyphen).getNormalized()).isEqualTo("blp1567");
        assertThat(SearchKeyword.from(spaced).getNormalized()).isEqualTo("blp1567");
        assertThat(SearchKeyword.from(joined).getNormalized()).isEqualTo("blp1567");
    }

    @Test
    @DisplayName("원문은_정규화와_무관하게_그대로_남는다")
    void 원문_보존() {
        // given
        final String raw = "BLP-1567";

        // when
        final SearchKeyword keyword = SearchKeyword.from(raw);

        // then
        // 이름 검색은 분석기를 타는 필드를 보므로 원문이 필요하다
        assertThat(keyword.getValue()).isEqualTo("BLP-1567");
    }

    @Test
    @DisplayName("글자와_숫자가_하나도_없으면_정규화_값이_비어_있다")
    void 정규화_결과_없음() {
        // given — 기호만 있는 검색어
        final SearchKeyword keyword = SearchKeyword.from("--");

        // when & then
        assertThat(keyword.hasNormalized()).isFalse();
        assertThat(keyword.getNormalized()).isEmpty();
    }
}
