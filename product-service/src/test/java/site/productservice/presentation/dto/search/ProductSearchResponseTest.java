package site.productservice.presentation.dto.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.productservice.application.dto.search.ProductSearchResult;
import site.productservice.domain.PressType;
import site.productservice.domain.search.ProductSearchHit;

/** 응답 record의 필드명 = JSON 필드명 = 다른 팀이 보는 API 명세. 필드명·enum→문자열 변환이 어긋나는 걸 막는다. */
class ProductSearchResponseTest {

    @Test
    @DisplayName("from은 api명세 카드 필드명으로 매핑한다 (pressType은 문자열)")
    void from_명세_필드명으로_매핑() {
        // given
        ProductSearchResult result = new ProductSearchResult(
                List.of(new ProductSearchHit(55L, "Abbey Road", "The Beatles", null, 1969, PressType.ORIGINAL, "UK")),
                0, 20, 1L, false);

        // when
        ProductSearchResponse response = ProductSearchResponse.from(result);

        // then
        ProductSearchResponse.Card card = response.content().get(0);
        assertThat(card.productId()).isEqualTo(55L);
        assertThat(card.title()).isEqualTo("Abbey Road");
        assertThat(card.artistName()).isEqualTo("The Beatles");
        assertThat(card.coverImageUrl()).isNull();
        assertThat(card.releaseYear()).isEqualTo(1969);
        assertThat(card.pressType()).isEqualTo("ORIGINAL");
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(1L);
        assertThat(response.hasNext()).isFalse();
    }
}
