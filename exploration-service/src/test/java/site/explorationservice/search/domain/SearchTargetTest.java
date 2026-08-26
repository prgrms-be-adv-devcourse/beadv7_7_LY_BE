package site.explorationservice.search.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.explorationservice.search.exception.UnsupportedSearchTargetException;

@DisplayName("검색 대상")
class SearchTargetTest {

    @Test
    @DisplayName("대상을_안_주면_이름_검색으로_본다")
    void 기본값_이름검색() {
        // given
        // when
        final SearchTarget target = SearchTarget.from(null);

        // then
        // 기존 호출은 searchBy를 안 보낸다. 여기가 NAME이 아니면 프론트의 모든 검색이 갈래를 잘못 탄다
        assertThat(target).isEqualTo(SearchTarget.NAME);
    }

    @Test
    @DisplayName("빈_문자열도_이름_검색으로_본다")
    void 빈_값_이름검색() {
        // given
        final String raw = "   ";

        // when
        final SearchTarget target = SearchTarget.from(raw);

        // then
        assertThat(target).isEqualTo(SearchTarget.NAME);
    }

    @Test
    @DisplayName("대소문자가_달라도_같은_대상으로_읽는다")
    void 대소문자_무시() {
        // given
        // when & then
        assertThat(SearchTarget.from("catalog")).isEqualTo(SearchTarget.CATALOG);
        assertThat(SearchTarget.from("CATALOG")).isEqualTo(SearchTarget.CATALOG);
        assertThat(SearchTarget.from(" Catalog ")).isEqualTo(SearchTarget.CATALOG);
    }

    @Test
    @DisplayName("모르는_값은_거절한다")
    void 미지원_대상_거절() {
        // given — catalog의 흔한 오타
        final String raw = "catlog";

        // when & then
        // 조용히 이름 검색으로 넘기면 호출한 쪽은 번호 검색이 된 줄 안다
        assertThatThrownBy(() -> SearchTarget.from(raw))
                .isInstanceOf(UnsupportedSearchTargetException.class);
    }
}
