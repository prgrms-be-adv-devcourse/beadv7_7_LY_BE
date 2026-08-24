package site.explorationservice.productindex.application;

import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.explorationservice.productindex.application.dto.ProductIndexCommand;

@DisplayName("상품 임베딩 텍스트 조립 (3-벡터)")
class ProductEmbeddingTemplateTest {

    private ProductIndexCommand command(final String genre, final String label,
        final Integer releaseYear, final String releaseCountry, final String pressType) {
        return new ProductIndexCommand(
            1L, "Kind of Blue", "Miles Davis", null, genre, label,
            releaseYear, releaseCountry, pressType, true, List.of(), List.of());
    }

    private ProductIndexCommand fullCommand() {
        return command("Jazz", "Columbia", 1959, "미국", "ORIGINAL");
    }

    @Test
    @DisplayName("identity는 장르·아티스트 순으로 담는다")
    void identity() {
        assertThat(ProductEmbeddingTemplate.buildIdentity(fullCommand()))
            .isEqualTo("Jazz · Miles Davis")
            .doesNotContain("Columbia", "1950", "미국", "ORIGINAL");
    }

    @Test
    @DisplayName("origin은 연대와 국가만 담는다")
    void origin() {
        assertThat(ProductEmbeddingTemplate.buildOrigin(fullCommand()))
            .isEqualTo("1950년대 · 미국")
            .doesNotContain("Miles Davis", "Jazz", "Columbia");
    }

    @Test
    @DisplayName("edition은 레이블과 프레스타입만 담고, pressType은 한국어로 바꾸지 않고 원본 값 그대로 쓴다")
    void edition() {
        assertThat(ProductEmbeddingTemplate.buildEdition(fullCommand()))
            .isEqualTo("Columbia · ORIGINAL")
            .doesNotContain("Miles Davis", "Jazz", "1950", "미국", "오리지널 프레스");
    }

    @Test
    @DisplayName("빈 필드는 통째로 빠지고 null이 새어나오지 않는다")
    void 빈_필드_제외() {
        final ProductIndexCommand empty = command(null, null, null, null, null);

        assertThat(ProductEmbeddingTemplate.buildIdentity(empty)).isEqualTo("Miles Davis");
        assertThat(ProductEmbeddingTemplate.buildOrigin(empty)).isEmpty();
        assertThat(ProductEmbeddingTemplate.buildEdition(empty)).isEmpty();
    }

    @Test
    @DisplayName("연도는 숫자가 아니라 연대로 바뀐다")
    void 연대_변환() {
        assertThat(ProductEmbeddingTemplate.buildOrigin(fullCommand()))
            .contains("1950년대")
            .doesNotContain("1959");
    }
}
