package site.coreservice.product.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductTest {

    private Product createSample() {
        return Product.of("CL 1355", "cl1355", 1L, "Kind of Blue", "kindofblue",
                "US", 1959, PressType.ORIGINAL, "LP", "Columbia", "Jazz", null, "설명");
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
