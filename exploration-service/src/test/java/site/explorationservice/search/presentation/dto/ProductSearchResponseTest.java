package site.explorationservice.search.presentation.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.explorationservice.search.application.dto.ProductSearchResult;
import site.explorationservice.search.domain.ProductSearchHit;

@DisplayName("상품 검색 응답")
class ProductSearchResponseTest {

    @Test
    @DisplayName("검색 결과의 표시 필드를 카드로 옮긴다")
    void 카드_변환() {
        // given
        final ProductSearchHit hit = new ProductSearchHit(42L, "별일 없이 산다", "장기하와 얼굴들",
            "https://img.example.com/42.jpg", 2009, "ORIGINAL");
        final ProductSearchResult result =
            new ProductSearchResult(List.of(hit), 0, 20, 1L, false);

        // when
        final ProductSearchResponse response = ProductSearchResponse.from(result);

        // then
        assertThat(response.content()).hasSize(1);
        final ProductSearchResponse.Card card = response.content().getFirst();
        assertThat(card.productId()).isEqualTo(42L);
        assertThat(card.title()).isEqualTo("별일 없이 산다");
        assertThat(card.artistName()).isEqualTo("장기하와 얼굴들");
        assertThat(card.coverImageUrl()).isEqualTo("https://img.example.com/42.jpg");
        assertThat(card.releaseYear()).isEqualTo(2009);
        assertThat(card.pressType()).isEqualTo("ORIGINAL");
    }

    @Test
    @DisplayName("발매연도가 비어 있으면 비운 채로 내보낸다")
    void 발매연도_없음() {
        // given
        // 표시 필드는 색인 경로 보완과 재색인이 끝나야 채워진다. 그전까지는 비어서 들어온다.
        final ProductSearchHit hit =
            new ProductSearchHit(42L, "별일 없이 산다", "장기하와 얼굴들", null, null, null);
        final ProductSearchResult result =
            new ProductSearchResult(List.of(hit), 0, 20, 1L, false);

        // when
        final ProductSearchResponse response = ProductSearchResponse.from(result);

        // then
        // 없는 값을 0으로 지어내면 화면에 0년으로 표시될 수 있다. 비어 있다는 사실을 그대로 전달한다.
        assertThat(response.content().getFirst().releaseYear()).isNull();
        assertThat(response.content().getFirst().pressType()).isNull();
    }

    @Test
    @DisplayName("페이지 정보를 그대로 옮긴다")
    void 페이지_정보_전달() {
        // given
        final ProductSearchResult result = new ProductSearchResult(List.of(), 2, 20, 45L, false);

        // when
        final ProductSearchResponse response = ProductSearchResponse.from(result);

        // then
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(45L);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.content()).isEmpty();
    }
}
