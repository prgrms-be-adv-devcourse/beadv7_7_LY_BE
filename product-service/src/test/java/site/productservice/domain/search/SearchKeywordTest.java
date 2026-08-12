package site.productservice.domain.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SearchKeywordTest {

    @Test
    @DisplayName("다중어_검색어를_공백_단위로_토큰화하고_토큰별로_정규화한다")
    void from_multiWord_tokenizes() {
        // given
        String raw = "김광석 부르기";

        // when
        SearchKeyword keyword = SearchKeyword.from(raw);

        // then
        assertThat(keyword.getTokens()).containsExactly("김광석", "부르기");
        assertThat(keyword.getWhole()).isEqualTo("김광석부르기");
    }

    @Test
    @DisplayName("전체_정규화값은_공백을_제거해_카탈로그_번호_매칭_키로_쓸_수_있다")
    void getWhole_catalogNumber_joins() {
        // given & when
        SearchKeyword keyword = SearchKeyword.from("CL 1355");

        // then
        assertThat(keyword.getWhole()).isEqualTo("cl1355");
        assertThat(keyword.getTokens()).containsExactly("cl", "1355");
    }

    @Test
    @DisplayName("정규화_후_비는_토큰은_버리고_중복_토큰은_한_번만_남긴다")
    void from_dropsEmptyAndDuplicateTokens() {
        // given & when
        SearchKeyword keyword = SearchKeyword.from("비틀즈 !!! 비틀즈");

        // then
        assertThat(keyword.getTokens()).containsExactly("비틀즈");
    }

    @Test
    @DisplayName("전부_기호뿐인_검색어는_비어있다고_판정한다")
    void isEmpty_symbolOnly_true() {
        // given & when & then
        assertThat(SearchKeyword.from("!!!").isEmpty()).isTrue();
        assertThat(SearchKeyword.from(null).isEmpty()).isTrue();
    }
}
