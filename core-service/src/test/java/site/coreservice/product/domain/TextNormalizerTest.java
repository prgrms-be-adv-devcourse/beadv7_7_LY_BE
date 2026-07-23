package site.coreservice.product.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TextNormalizerTest {

    @Test
    @DisplayName("한글은 그대로 보존한다")
    void normalize_한글_보존() {
        // given & when & then
        assertThat(TextNormalizer.normalize("비틀즈")).isEqualTo("비틀즈");
    }

    @Test
    @DisplayName("대소문자·공백·기호를 통일해 제거한다")
    void normalize_표기_통일() {
        // given & when & then
        assertThat(TextNormalizer.normalize("The Beatles")).isEqualTo("thebeatles");
        assertThat(TextNormalizer.normalize("PCS-7088")).isEqualTo("pcs7088");
        assertThat(TextNormalizer.normalize("비틀즈 1집")).isEqualTo("비틀즈1집");
    }

    @Test
    @DisplayName("전각 문자는 반각으로 통일된다")
    void normalize_전각_통일() {
        // given & when & then
        assertThat(TextNormalizer.normalize("ＡＢＢＥＹ")).isEqualTo("abbey");
    }

    @Test
    @DisplayName("기호만 있으면 null을 반환한다 (빈 문자열 저장 금지)")
    void normalize_기호만이면_null() {
        // given & when & then
        assertThat(TextNormalizer.normalize("!!!")).isNull();
        assertThat(TextNormalizer.normalize("   ")).isNull();
        assertThat(TextNormalizer.normalize("")).isNull();
    }

    @Test
    @DisplayName("null 입력은 null을 반환한다")
    void normalize_null이면_null() {
        // given & when & then
        assertThat(TextNormalizer.normalize(null)).isNull();
    }

    @Test
    @DisplayName("실행 환경 로케일과 무관하게 같은 결과를 낸다 (터키어 I 문제)")
    void normalize_로케일_무관() {
        // given
        Locale original = Locale.getDefault();
        Locale.setDefault(Locale.of("tr", "TR"));
        try {
            // when & then — 터키 로케일이면 기본 toLowerCase는 I를 ı(점 없는 i)로 바꾼다
            assertThat(TextNormalizer.normalize("MILES")).isEqualTo("miles");
        } finally {
            Locale.setDefault(original);
        }
    }
}
