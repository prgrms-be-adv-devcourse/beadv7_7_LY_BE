package site.explorationservice.search.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.explorationservice.search.exception.UnsupportedSearchTargetException;

@DisplayName("검색 대상")
class SearchTargetTest {

    @Test
    @DisplayName("대상을 안 주면 이름 검색으로 본다")
    void 기본값_이름검색() {
        // given
        // when
        final SearchTarget target = SearchTarget.from(null);

        // then
        // 기존 호출은 searchBy를 안 보낸다. 여기가 NAME이 아니면 프론트의 모든 검색이 갈래를 잘못 탄다
        assertThat(target).isEqualTo(SearchTarget.NAME);
    }

    @Test
    @DisplayName("빈 문자열도 이름 검색으로 본다")
    void 빈_값_이름검색() {
        // given
        final String raw = "   ";

        // when
        final SearchTarget target = SearchTarget.from(raw);

        // then
        assertThat(target).isEqualTo(SearchTarget.NAME);
    }

    @Test
    @DisplayName("이름을 그대로 주면 이름 검색으로 본다")
    void 이름_대상() {
        // given
        final String raw = "name";

        // when
        final SearchTarget target = SearchTarget.from(raw);

        // then
        // 프론트가 선택기를 붙이면 이 값을 직접 보낸다. 상수 이름이 바뀌면 여기서 걸린다
        assertThat(target).isEqualTo(SearchTarget.NAME);
    }

    @Test
    @DisplayName("대소문자가 달라도 같은 대상으로 읽는다")
    void 대소문자_무시() {
        // given
        // when & then
        assertThat(SearchTarget.from("catalog")).isEqualTo(SearchTarget.CATALOG);
        assertThat(SearchTarget.from("CATALOG")).isEqualTo(SearchTarget.CATALOG);
        assertThat(SearchTarget.from(" Catalog ")).isEqualTo(SearchTarget.CATALOG);
    }

    @Test
    @DisplayName("모르는 값은 거절한다")
    void 미지원_대상_거절() {
        // given — catalog의 흔한 오타
        final String raw = "catlog";

        // when & then
        // 조용히 이름 검색으로 넘기면 호출한 쪽은 번호 검색이 된 줄 안다
        assertThatThrownBy(() -> SearchTarget.from(raw))
                .isInstanceOf(UnsupportedSearchTargetException.class);
    }
}
