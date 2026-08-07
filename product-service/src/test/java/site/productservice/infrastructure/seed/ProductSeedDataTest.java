package site.productservice.infrastructure.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.productservice.domain.TextNormalizer;

/**
 * 시드 원장 자체의 정합성 검사. 원장은 손으로 관리하는 데이터라 오타 하나로
 * 조용히 잘못 적재될 수 있어, 그 실수들을 컴파일 다음 단계에서 잡는다.
 */
class ProductSeedDataTest {

    @Test
    @DisplayName("모든 상품의 아티스트명은 아티스트 원장에 있다 — 없으면 로더가 기동 중 예외를 던진다")
    void 상품의_아티스트가_원장에_존재() {
        // given
        Set<String> artistNames = ProductSeedData.ARTISTS.stream()
                .map(ProductSeedData.ArtistSeed::name)
                .collect(Collectors.toSet());

        // when & then
        assertThat(ProductSeedData.PRODUCTS)
                .allSatisfy(product -> assertThat(artistNames).contains(product.artistName()));
    }

    @Test
    @DisplayName("아티스트 이름은 표기 통일 후에도 겹치지 않는다")
    void 아티스트_이름_중복_없음() {
        // given
        List<String> normalizedNames = ProductSeedData.ARTISTS.stream()
                .map(artist -> TextNormalizer.normalize(artist.name()))
                .toList();

        // when & then
        assertThat(normalizedNames).doesNotContainNull().doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("카탈로그번호 자연키(번호+포맷+국가)는 겹치지 않는다 — 겹치면 뒤 상품이 조용히 앞 상품에 합쳐진다")
    void 자연키_중복_없음() {
        // given
        List<String> naturalKeys = ProductSeedData.PRODUCTS.stream()
                .filter(product -> product.catalogNumber() != null)
                .map(product -> TextNormalizer.normalize(product.catalogNumber())
                        + "|" + product.format() + "|" + product.releaseCountry())
                .toList();

        // when & then
        assertThat(naturalKeys).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("카탈로그번호 없는 상품끼리도 예비 기준(제목+아티스트+연도+국가+포맷+프레스)이 겹치지 않는다")
    void 폴백_키_중복_없음() {
        // given
        List<String> fallbackKeys = ProductSeedData.PRODUCTS.stream()
                .filter(product -> product.catalogNumber() == null)
                .map(product -> TextNormalizer.normalize(product.title()) + "|" + product.artistName()
                        + "|" + product.releaseYear() + "|" + product.releaseCountry()
                        + "|" + product.format() + "|" + product.pressType())
                .toList();

        // when & then
        assertThat(fallbackKeys).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("기준가 있는 상품은 카탈로그번호도 있고 값이 양수다 — 시세 시드가 번호로 상품을 찾기 때문")
    void 기준가_상품은_카탈로그번호_필수() {
        // when & then
        assertThat(ProductSeedData.PRODUCTS)
                .filteredOn(product -> product.basePrice() != null)
                .allSatisfy(product -> {
                    assertThat(product.catalogNumber()).isNotNull();
                    assertThat(product.basePrice()).isPositive();
                });
    }

    @Test
    @DisplayName("시세 대상은 50개 이상이되, 시세 없는 상품도 남아 있다 (빈 배열 응답 시연용)")
    void 시세_대상_규모와_빈_상품_보존() {
        // given
        long pricedCount = ProductSeedData.PRODUCTS.stream()
                .filter(product -> product.basePrice() != null)
                .count();

        // when & then
        assertThat(pricedCount).isGreaterThanOrEqualTo(50);
        assertThat(ProductSeedData.PRODUCTS.size() - pricedCount).isPositive();
    }

    @Test
    @DisplayName("데모 규모 — 아티스트 50팀 이상, 상품 100건 이상 (한국·해외 모두 포함)")
    void 데모_규모_충족() {
        // when & then
        assertThat(ProductSeedData.ARTISTS.size()).isGreaterThanOrEqualTo(50);
        assertThat(ProductSeedData.PRODUCTS.size()).isGreaterThanOrEqualTo(100);
        assertThat(ProductSeedData.PRODUCTS)
                .anyMatch(product -> product.releaseCountry().equals("Korea"))
                .anyMatch(product -> !product.releaseCountry().equals("Korea"));
    }
}
