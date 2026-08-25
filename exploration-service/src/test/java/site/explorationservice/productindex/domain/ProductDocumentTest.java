package site.explorationservice.productindex.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("색인 문서")
class ProductDocumentTest {

    @Test
    @DisplayName("마스터 번호가 있으면 그 값을 그룹 키로 쓴다")
    void 그룹키_마스터() {
        // given
        final Long discogsMasterId = 12345L;
        final Long productId = 999L;

        // when
        final String groupKey = ProductDocument.groupKeyOf(discogsMasterId, productId);

        // then
        assertThat(groupKey).isEqualTo("12345");
    }

    @Test
    @DisplayName("마스터 번호가 없으면 상품 번호 앞에 p를 붙인다")
    void 그룹키_대체() {
        // given
        final Long productId = 12345L;

        // when
        final String groupKey = ProductDocument.groupKeyOf(null, productId);

        // then
        assertThat(groupKey).isEqualTo("p12345");
    }

    @Test
    @DisplayName("대체 그룹 키는 같은 숫자의 마스터 번호와 겹치지 않는다")
    void 그룹키_충돌_없음() {
        // given
        // 마스터 번호가 없는 상품이 실데이터의 22.94%다. 접두어가 없으면 상품 번호 12345인 상품과
        // 마스터 번호 12345인 앨범이 같은 그룹으로 묶여 서로 다른 앨범이 하나로 접힌다.
        final String fromProductId = ProductDocument.groupKeyOf(null, 12345L);

        // when
        final String fromMasterId = ProductDocument.groupKeyOf(12345L, 777L);

        // then
        assertThat(fromProductId).isNotEqualTo(fromMasterId);
    }
}
