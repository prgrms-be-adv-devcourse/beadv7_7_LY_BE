package site.coreservice.product.presentation.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.coreservice.product.application.dto.ProductDetailResult;
import site.coreservice.product.domain.PressType;

/**
 * Response record 컴포넌트명 = JSON 필드명 = 타팀이 소비하는 api명세 표면.
 * 이름 변경(catalogNumber→catalogNo)·enum→String 변환이 조용히 어긋나는 회귀를 막는다.
 */
class ProductDetailResponseTest {

    @Test
    @DisplayName("from은 api명세 필드명으로 매핑한다 (catalogNo·country·coverImageUrl, pressType은 문자열)")
    void from_명세_필드명으로_매핑() {
        // given
        ProductDetailResult result = new ProductDetailResult(55L, "PCS 7088", "Abbey Road",
                new ProductDetailResult.ArtistResult(3L, "The Beatles", List.of("비틀즈")),
                "Apple Records", "UK", 1969, PressType.ORIGINAL, "LP", "Rock",
                "https://cdn.example/55.jpg", "1969년 영국 오리지널 프레싱");

        // when
        ProductDetailResponse response = ProductDetailResponse.from(result);

        // then
        assertThat(response.productId()).isEqualTo(55L);
        assertThat(response.catalogNo()).isEqualTo("PCS 7088");
        assertThat(response.title()).isEqualTo("Abbey Road");
        assertThat(response.artist().artistId()).isEqualTo(3L);
        assertThat(response.artist().name()).isEqualTo("The Beatles");
        assertThat(response.artist().aliases()).containsExactly("비틀즈");
        assertThat(response.label()).isEqualTo("Apple Records");
        assertThat(response.country()).isEqualTo("UK");
        assertThat(response.releaseYear()).isEqualTo(1969);
        assertThat(response.pressType()).isEqualTo("ORIGINAL");
        assertThat(response.format()).isEqualTo("LP");
        assertThat(response.genre()).isEqualTo("Rock");
        assertThat(response.coverImageUrl()).isEqualTo("https://cdn.example/55.jpg");
        assertThat(response.description()).isEqualTo("1969년 영국 오리지널 프레싱");
    }
}
