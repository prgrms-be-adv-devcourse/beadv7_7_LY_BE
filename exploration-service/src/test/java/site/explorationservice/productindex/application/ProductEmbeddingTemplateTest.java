package site.explorationservice.productindex.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import site.explorationservice.productindex.application.dto.ProductIndexCommand;

@DisplayName("상품 임베딩 텍스트 조립")
class ProductEmbeddingTemplateTest {

    private ProductIndexCommand command(final String genre, final String label,
        final Integer releaseYear, final String releaseCountry, final String pressType) {
        return new ProductIndexCommand(
            1L, "Kind of Blue", "Miles Davis", genre, label,
            releaseYear, releaseCountry, pressType, true);
    }

    private ProductIndexCommand fullCommand() {
        return command("Jazz", "Columbia", 1959, "미국", "ORIGINAL");
    }

    @Nested
    @DisplayName("두 형식 공통")
    class Common {

        /**
         * 제목을 빼는 게 이 템플릿의 핵심 결정이다. 위시리스트 벡터를 평균낼 때 서로 무관한 제목들이 섞여 신호를 희석시키기 때문인데, 무심코 다시 넣기 쉬운 부분이라
         * 두 형식 모두 고정해 둔다.
         */
        @Test
        @DisplayName("앨범 제목은 넣지 않는다")
        void 제목_제외() {
            for (final ProductEmbeddingTemplate template : ProductEmbeddingTemplate.values()) {
                assertThat(template.build(fullCommand())).doesNotContain("Kind of Blue");
            }
        }

        @Test
        @DisplayName("연도는 숫자가 아니라 연대로 바뀐다")
        void 연대_변환() {
            for (final ProductEmbeddingTemplate template : ProductEmbeddingTemplate.values()) {
                final String text = template.build(fullCommand());
                assertThat(text).contains("1950년대");
                assertThat(text).doesNotContain("1959");
            }
        }

        @Test
        @DisplayName("pressType은 enum 이름이 아니라 한국어로 바뀐다")
        void 프레스타입_변환() {
            for (final ProductEmbeddingTemplate template : ProductEmbeddingTemplate.values()) {
                assertThat(template.build(fullCommand()))
                    .contains("오리지널 프레스")
                    .doesNotContain("ORIGINAL");
                assertThat(template.build(command("Jazz", "Columbia", 1990, "일본", "REISSUE")))
                    .contains("재발매반")
                    .doesNotContain("REISSUE");
            }
        }

        @Test
        @DisplayName("모르는 pressType 값은 지우지 않고 그대로 둔다")
        void 알수없는_프레스타입() {
            for (final ProductEmbeddingTemplate template : ProductEmbeddingTemplate.values()) {
                assertThat(template.build(command("Jazz", "Columbia", 1959, "미국", "REMASTER")))
                    .contains("REMASTER");
            }
        }

        /**
         * "장르: null" 같은 문구가 벡터에 섞이면 안 된다.
         */
        @Test
        @DisplayName("비어 있는 필드는 통째로 빠지고 null이 문자열로 새어나오지 않는다")
        void 빈_필드_제외() {
            for (final ProductEmbeddingTemplate template : ProductEmbeddingTemplate.values()) {
                final String text = template.build(command(null, null, null, null, null));

                assertThat(text).doesNotContain("null");
                assertThat(text).contains("Miles Davis");
            }
        }

        @Test
        @DisplayName("구분자만 남는 빈 줄이 생기지 않는다")
        void 빈_줄_없음() {
            for (final ProductEmbeddingTemplate template : ProductEmbeddingTemplate.values()) {
                final String text = template.build(command(null, null, null, null, null));

                assertThat(text.lines()).allSatisfy(
                    line -> assertThat(line).isNotBlank());
                assertThat(text).doesNotStartWith("·").doesNotEndWith("·");
            }
        }
    }

    @Nested
    @DisplayName("COMPACT")
    class Compact {

        @Test
        @DisplayName("레이블 없이 값만 나열한다")
        void 조립() {
            final String text = ProductEmbeddingTemplate.COMPACT.build(fullCommand());

            assertThat(text).isEqualTo("""
                Miles Davis · Jazz · Columbia
                1950년대 · 미국 · 오리지널 프레스""");
        }
    }

    @Nested
    @DisplayName("LABELED")
    class Labeled {

        @Test
        @DisplayName("모호한 값에 레이블을 붙인다")
        void 조립() {
            final String text = ProductEmbeddingTemplate.LABELED.build(fullCommand());

            assertThat(text).isEqualTo("""
                아티스트: Miles Davis
                장르: Jazz · 레이블: Columbia
                1950년대 · 미국 · 오리지널 프레스""");
        }

        @Test
        @DisplayName("값이 없는 항목은 레이블까지 함께 빠진다")
        void 빈_항목은_레이블도_빠진다() {
            final String text = ProductEmbeddingTemplate.LABELED
                .build(command(null, "Columbia", 1959, "미국", "ORIGINAL"));

            assertThat(text).doesNotContain("장르");
            assertThat(text).contains("레이블: Columbia");
        }
    }

    @Nested
    @DisplayName("3-벡터 보조 텍스트")
    class ThreeVector {

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
    }
}
