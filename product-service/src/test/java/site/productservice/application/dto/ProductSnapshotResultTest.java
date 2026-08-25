package site.productservice.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import site.productservice.domain.Artist;
import site.productservice.domain.PressType;
import site.productservice.domain.Product;

@DisplayName("상품 스냅샷 변환")
class ProductSnapshotResultTest {

    @Test
    @DisplayName("카탈로그 번호와 마스터 번호를 원본 그대로 담는다")
    void 번호_전달() {
        // given
        Product product = productWith("BLP-1567", 4001L);
        Artist artist = Artist.of("John Coltrane");

        // when
        ProductSnapshotResult result = ProductSnapshotResult.of(product, artist);

        // then
        // 색인하는 쪽이 표기를 다듬으므로 여기서는 손대지 않고 그대로 넘긴다.
        // 마스터 번호는 엔티티에서 discogsMasterId라는 다른 이름으로 온다 — 이름이 어긋나면
        // 응답 필드가 늘 비어 판본 묶기가 통째로 무력해진다.
        assertThat(result.catalogNumber()).isEqualTo("BLP-1567");
        assertThat(result.masterId()).isEqualTo(4001L);
    }

    @Test
    @DisplayName("마스터 번호가 없는 상품은 masterId가 null이다")
    void 마스터_없음() {
        // given
        // 실데이터의 22.94%가 마스터 번호를 갖고 있지 않다
        Product product = productWith("BLP-1567", null);
        Artist artist = Artist.of("John Coltrane");

        // when
        ProductSnapshotResult result = ProductSnapshotResult.of(product, artist);

        // then
        assertThat(result.masterId()).isNull();
    }

    /**
     * discogsMasterId는 Discogs 원본을 SQL로 직접 적재하는 컬럼이라 엔티티 팩토리로는 넣을 수 없다.
     */
    private Product productWith(String catalogNumber, Long masterId) {
        Product product = Product.of(catalogNumber, 1L, "Blue Train", "US", 1957,
                PressType.ORIGINAL, "LP", "Blue Note", "Jazz", null, null);
        ReflectionTestUtils.setField(product, "discogsMasterId", masterId);
        return product;
    }
}
