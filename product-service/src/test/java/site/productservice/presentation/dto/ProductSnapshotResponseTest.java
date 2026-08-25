package site.productservice.presentation.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.productservice.application.dto.ProductSnapshotResult;
import site.productservice.domain.PressType;

/**
 * 내부 getProductSnapshot 응답(2-1)은 경매(07)·주문(06)이 소비하는 계약 표면.
 * 필드명·enum→String 변환이 조용히 어긋나는 회귀를 막는다.
 */
class ProductSnapshotResponseTest {

    @Test
    @DisplayName("from은 스냅샷 필드를 그대로 매핑하고 pressType은 문자열로 변환한다")
    void from_스냅샷_필드_매핑() {
        // given: 비활성 + 커버 없음 — 경계 값 조합
        ProductSnapshotResult result = new ProductSnapshotResult(55L, "Abbey Road", "The Beatles",
                null, "Rock", "Apple", PressType.ORIGINAL, 1969, "영국", false, null,
                "PCS 7088", 24047L);

        // when
        ProductSnapshotResponse response = ProductSnapshotResponse.from(result);

        // then
        assertThat(response.productId()).isEqualTo(55L);
        assertThat(response.title()).isEqualTo("Abbey Road");
        assertThat(response.artistName()).isEqualTo("The Beatles");
        assertThat(response.coverImageUrl()).isNull();
        assertThat(response.genre()).isEqualTo("Rock");
        assertThat(response.label()).isEqualTo("Apple");
        assertThat(response.pressType()).isEqualTo("ORIGINAL");
        assertThat(response.releaseYear()).isEqualTo(1969);
        assertThat(response.releaseCountry()).isEqualTo("영국");
        assertThat(response.active()).isFalse();
        assertThat(response.mergedIntoId()).isNull();
        // 검색 색인이 소비하는 두 필드. 이름이 어긋나면 예외가 아니라 조용히 null이 된다
        assertThat(response.catalogNumber()).isEqualTo("PCS 7088");
        assertThat(response.masterId()).isEqualTo(24047L);
    }
}
