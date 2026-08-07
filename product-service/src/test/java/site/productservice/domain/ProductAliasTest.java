package site.productservice.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductAliasTest {

    @Test
    @DisplayName("생성 시 정규화 값을 내부에서 계산한다 (외부 주입 불가)")
    void of_정규화_내부_계산() {
        // given & when
        ProductAlias alias = ProductAlias.of(5L, "Abbey Road");

        // then
        assertThat(alias.getName()).isEqualTo("Abbey Road");
        assertThat(alias.getNormalizedName()).isEqualTo("abbeyroad");
    }

    @Test
    @DisplayName("정규화하면 아무것도 남지 않는 별칭은 거부한다 (검색 불가능한 별칭)")
    void of_기호만이면_예외() {
        // given & when & then
        assertThatThrownBy(() -> ProductAlias.of(5L, "!!!"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
