package site.productservice.presentation.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.productservice.application.dto.ProductDetailResult;
import site.productservice.domain.PressType;

/**
 * 응답 record의 필드명이 그대로 JSON 필드명이 되고, 그 JSON이 다른 팀이 보는 API 명세다.
 * 필드명(catalogNumber→catalogNo 같은 변환)이나 enum→문자열 변환이 모르는 사이 어긋나는 걸 막는 테스트.
 */
class ProductDetailResponseTest {

    @Test
    @DisplayName("from은 api명세 필드명으로 매핑한다 (catalogNo·country·coverImageUrl, pressType은 문자열)")
    void from_명세_필드명으로_매핑() {
        // given
        ProductDetailResult result = new ProductDetailResult(55L, "PCS 7088", "Abbey Road",
                new ProductDetailResult.ArtistResult(3L, "The Beatles"),
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
