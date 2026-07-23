package site.coreservice.product.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductTest {

    private Product createSample() {
        return Product.of("CL 1355", 1L, "Kind of Blue", "US", 1959,
                PressType.ORIGINAL, "LP", "Columbia", "Jazz", null, "설명");
    }

    @Test
    @DisplayName("생성 시 정규화 값을 내부에서 계산한다")
    void of_정규화_내부_계산() {
        // given & when
        Product product = Product.of("CL 1355", 1L, "Kind of Blue", "US", 1959,
                PressType.ORIGINAL, "LP", "Columbia", "Jazz", null, "설명");

        // then
        assertThat(product.getNormalizedCatalogNumber()).isEqualTo("cl1355");
        assertThat(product.getNormalizedTitle()).isEqualTo("kindofblue");
    }

    @Test
    @DisplayName("카탈로그번호가 null이나 공백이면 원본·정규화 모두 null로 저장한다 (정상 상태)")
    void of_카탈로그번호_없으면_null_통일() {
        // given & when
        Product noNumber = Product.of(null, 1L, "Kum Back", "US", 1969,
                PressType.ORIGINAL, "LP", null, "Rock", null, "부틀렉");
        Product blankNumber = Product.of("  ", 1L, "Kum Back", "US", 1969,
                PressType.ORIGINAL, "LP", null, "Rock", null, "부틀렉");

        // then
        assertThat(noNumber.getCatalogNumber()).isNull();
        assertThat(noNumber.getNormalizedCatalogNumber()).isNull();
        assertThat(blankNumber.getCatalogNumber()).isNull();
        assertThat(blankNumber.getNormalizedCatalogNumber()).isNull();
    }

    @Test
    @DisplayName("카탈로그번호가 기호만이면 원본·정규화 모두 null로 저장한다 (없음 취급)")
    void of_카탈로그번호_기호만이면_원본도_null() {
        // given & when
        Product product = Product.of("---", 1L, "Kum Back", "US", 1969,
                PressType.ORIGINAL, "LP", null, "Rock", null, "부틀렉");

        // then
        assertThat(product.getCatalogNumber()).isNull();
        assertThat(product.getNormalizedCatalogNumber()).isNull();
    }

    @Test
    @DisplayName("정규화하면 아무것도 남지 않는 제목은 거부한다")
    void of_제목_기호만이면_예외() {
        // given & when & then
        assertThatThrownBy(() -> Product.of("CL 1355", 1L, "!!!", "US", 1959,
                PressType.ORIGINAL, "LP", null, "Rock", null, "설명"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("updateDescriptive는 서술 속성만 바꾸고 식별 속성은 그대로 둔다")
    void updateDescriptive_서술속성만_변경하고_식별속성은_불변() {
        // given
        Product product = createSample();

        // when
        product.updateDescriptive("Columbia Records", "Modal Jazz", "cover.jpg", "새 설명");

        // then: 서술 속성은 바뀐다
        assertThat(product.getLabel()).isEqualTo("Columbia Records");
        assertThat(product.getGenre()).isEqualTo("Modal Jazz");
        assertThat(product.getCoverImage()).isEqualTo("cover.jpg");
        assertThat(product.getDescription()).isEqualTo("새 설명");

        // then: 식별 속성은 그대로 (바뀌면 다른 릴리스가 되므로)
        assertThat(product.getCatalogNumber()).isEqualTo("CL 1355");
        assertThat(product.getNormalizedCatalogNumber()).isEqualTo("cl1355");
        assertThat(product.getArtistId()).isEqualTo(1L);
        assertThat(product.getReleaseCountry()).isEqualTo("US");
        assertThat(product.getReleaseYear()).isEqualTo(1959);
        assertThat(product.getPressType()).isEqualTo(PressType.ORIGINAL);
        assertThat(product.getFormat()).isEqualTo("LP");
        assertThat(product.getTitle()).isEqualTo("Kind of Blue");
        assertThat(product.getNormalizedTitle()).isEqualTo("kindofblue");
    }

    @Test
    @DisplayName("deactivate는 active를 false로 바꾼다 (soft delete)")
    void deactivate_active를_false로_바꾼다() {
        // given
        Product product = createSample();
        assertThat(product.isActive()).isTrue();

        // when
        product.deactivate();

        // then
        assertThat(product.isActive()).isFalse();
    }
}
