package site.explorationservice.searchlog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.explorationservice.searchlog.exception.InvalidSearchClickException;

@DisplayName("클릭 기록")
class SearchClickLogTest {

    @Test
    @DisplayName("검색 식별자와 상품 번호, 순위가 모두 있으면 기록을 만든다")
    void 값이_모두_있으면_생성된다() {
        // given
        final String searchId = "0f2c-search-id";

        // when
        final SearchClickLog clickLog = SearchClickLog.of(searchId, 12345L, 3);

        // then
        assertThat(clickLog.searchId()).isEqualTo(searchId);
        assertThat(clickLog.productId()).isEqualTo(12345L);
        assertThat(clickLog.rank()).isEqualTo(3);
        assertThat(clickLog.clickedAt()).isNotNull();
    }

    @Test
    @DisplayName("검색 식별자가 비어 있으면 예외를 던진다")
    void 검색_식별자가_비면_예외() {
        // given
        final String blankSearchId = "   ";

        // when & then
        assertThatThrownBy(() -> SearchClickLog.of(blankSearchId, 12345L, 1))
            .isInstanceOf(InvalidSearchClickException.class);
    }

    @Test
    @DisplayName("검색 식별자가 없으면 예외를 던진다")
    void 검색_식별자가_없으면_예외() {
        // given
        final String nullSearchId = null;

        // when & then
        assertThatThrownBy(() -> SearchClickLog.of(nullSearchId, 12345L, 1))
            .isInstanceOf(InvalidSearchClickException.class);
    }

    @Test
    @DisplayName("상품 번호가 없으면 예외를 던진다")
    void 상품_번호가_없으면_예외() {
        // given
        final Long nullProductId = null;

        // when & then
        assertThatThrownBy(() -> SearchClickLog.of("0f2c-search-id", nullProductId, 1))
            .isInstanceOf(InvalidSearchClickException.class);
    }

    @Test
    @DisplayName("순위가 1보다 작으면 예외를 던진다")
    void 순위가_1_미만이면_예외() {
        // given
        final int zeroRank = 0;

        // when & then
        assertThatThrownBy(() -> SearchClickLog.of("0f2c-search-id", 12345L, zeroRank))
            .isInstanceOf(InvalidSearchClickException.class);
    }

    @Test
    @DisplayName("순위가 1이면 경계값으로 통과한다")
    void 순위가_1이면_통과() {
        // given
        final int firstRank = 1;

        // when
        final SearchClickLog clickLog = SearchClickLog.of("0f2c-search-id", 12345L, firstRank);

        // then
        assertThat(clickLog.rank()).isEqualTo(firstRank);
    }
}
