package site.coreservice.product.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ArtistAliasTest {

    @Test
    @DisplayName("생성 시 정규화 값을 내부에서 계산한다 (외부 주입 불가)")
    void of_정규화_내부_계산() {
        // given & when
        ArtistAlias alias = ArtistAlias.of(3L, "The Beatles");

        // then
        assertThat(alias.getName()).isEqualTo("The Beatles");
        assertThat(alias.getNormalizedName()).isEqualTo("thebeatles");
    }

    @Test
    @DisplayName("정규화하면 아무것도 남지 않는 별칭은 거부한다 (검색 불가능한 별칭)")
    void of_기호만이면_예외() {
        // given & when & then
        assertThatThrownBy(() -> ArtistAlias.of(3L, "!!!"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
