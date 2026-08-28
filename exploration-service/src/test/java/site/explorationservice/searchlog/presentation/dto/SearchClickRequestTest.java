package site.explorationservice.searchlog.presentation.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.explorationservice.searchlog.domain.SearchClickLog;
import site.explorationservice.searchlog.exception.InvalidSearchClickException;

@DisplayName("클릭 기록 요청")
class SearchClickRequestTest {

    @Test
    @DisplayName("요청을 클릭 기록으로 바꾼다")
    void 요청을_기록으로_바꾼다() {
        // given
        final SearchClickRequest request = new SearchClickRequest("0f2c-search-id", 12345L, 3);

        // when
        final SearchClickLog clickLog = request.toSearchClickLog();

        // then
        assertThat(clickLog.searchId()).isEqualTo("0f2c-search-id");
        assertThat(clickLog.productId()).isEqualTo(12345L);
        assertThat(clickLog.rank()).isEqualTo(3);
        assertThat(clickLog.clickedAt()).isNotNull();
    }

    @Test
    @DisplayName("순위를 보내지 않으면 변환 단계에서 예외를 던진다")
    void 순위가_없으면_예외() {
        // given
        final SearchClickRequest request = new SearchClickRequest("0f2c-search-id", 12345L, null);

        // when & then
        // 저장은 다른 스레드에서 돌기 때문에, 여기서 막지 않으면 잘못된 요청에도 성공 응답이 나간다
        assertThatThrownBy(request::toSearchClickLog)
            .isInstanceOf(InvalidSearchClickException.class);
    }

    @Test
    @DisplayName("검색 식별자를 보내지 않으면 변환 단계에서 예외를 던진다")
    void 검색_식별자가_없으면_예외() {
        // given
        final SearchClickRequest request = new SearchClickRequest(null, 12345L, 3);

        // when & then
        assertThatThrownBy(request::toSearchClickLog)
            .isInstanceOf(InvalidSearchClickException.class);
    }

    @Test
    @DisplayName("상품 번호를 보내지 않으면 변환 단계에서 예외를 던진다")
    void 상품_번호가_없으면_예외() {
        // given
        final SearchClickRequest request = new SearchClickRequest("0f2c-search-id", null, 3);

        // when & then
        assertThatThrownBy(request::toSearchClickLog)
            .isInstanceOf(InvalidSearchClickException.class);
    }

    @Test
    @DisplayName("순위가 1보다 작으면 변환 단계에서 예외를 던진다")
    void 순위가_1_미만이면_예외() {
        // given
        // 순위는 1부터 센다. 0은 값을 안 보낸 것과 구분되지 않아 잘못된 기록이 된다
        final SearchClickRequest request = new SearchClickRequest("0f2c-search-id", 12345L, 0);

        // when & then
        assertThatThrownBy(request::toSearchClickLog)
            .isInstanceOf(InvalidSearchClickException.class);
    }
}
