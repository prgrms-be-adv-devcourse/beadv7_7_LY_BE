package site.coreservice.product.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.coreservice.product.domain.ProductSearchPage;

/** hasNext 계산의 경계 — 곱셈이 int로 넘치면 극단 page 값에서 부호가 뒤집혀 잘못된 hasNext가 나온다. */
class ProductSearchResultTest {

    @Test
    @DisplayName("극단적으로 큰 page 값에서도 hasNext 계산이 넘치지 않는다 (long 산술)")
    void of_큰_page에서도_hasNext_정상() {
        // given & when — int 곱셈이면 (page+1)*size가 음수로 넘쳐 true가 나와버리는 값
        ProductSearchResult result = ProductSearchResult.of(
                new ProductSearchPage(List.of(), 10L), Integer.MAX_VALUE - 1, 100);

        // then
        assertThat(result.hasNext()).isFalse();
    }
}
