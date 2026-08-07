package site.productservice.domain.price;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MediaConditionTest {

    @Test
    @DisplayName("여섯 등급 문자열을 각각 대응하는 enum으로 변환한다")
    void from_여섯_등급_문자열을_enum으로_변환() {
        // given-when-then
        assertThat(MediaCondition.from("MINT")).isEqualTo(MediaCondition.MINT);
        assertThat(MediaCondition.from("NEAR_MINT")).isEqualTo(MediaCondition.NEAR_MINT);
        assertThat(MediaCondition.from("VERY_GOOD_PLUS")).isEqualTo(MediaCondition.VERY_GOOD_PLUS);
        assertThat(MediaCondition.from("VERY_GOOD")).isEqualTo(MediaCondition.VERY_GOOD);
        assertThat(MediaCondition.from("GOOD")).isEqualTo(MediaCondition.GOOD);
        assertThat(MediaCondition.from("POOR")).isEqualTo(MediaCondition.POOR);
    }

    @Test
    @DisplayName("null·미지 문자열·소문자·구 축약 표기는 예외를 던진다")
    void from_잘못된_입력은_예외() {
        // given-when-then
        assertThatThrownBy(() -> MediaCondition.from(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MediaCondition.from("SEALED")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MediaCondition.from("mint")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MediaCondition.from("NM")).isInstanceOf(IllegalArgumentException.class);
    }
}
