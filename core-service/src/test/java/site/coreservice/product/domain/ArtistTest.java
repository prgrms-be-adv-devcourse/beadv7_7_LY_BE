package site.coreservice.product.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ArtistTest {

    @Test
    @DisplayName("생성 시 정규화된 이름을 내부에서 계산한다")
    void of_정규화_내부_계산() {
        // given & when
        Artist artist = Artist.of("The Beatles");

        // then
        assertThat(artist.getName()).isEqualTo("The Beatles");
        assertThat(artist.getNormalizedName()).isEqualTo("thebeatles");
    }

    @Test
    @DisplayName("정규화하면 아무것도 남지 않는 이름은 거부한다")
    void of_기호만이면_예외() {
        // given & when & then
        assertThatThrownBy(() -> Artist.of("!!!"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
