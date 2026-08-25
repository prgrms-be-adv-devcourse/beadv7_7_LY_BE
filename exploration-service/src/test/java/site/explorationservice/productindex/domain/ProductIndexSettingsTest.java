package site.explorationservice.productindex.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 인덱스 설정 파일은 인덱스를 만드는 시점에만 읽힌다. 내용이 틀려도 애플리케이션은 그냥 뜨고,
 * 잘못된 분석기로 색인이 끝난 뒤에야 드러난다. 그때는 인덱스를 새로 만들어 전체 재색인해야 한다.
 * ES 없이 파일만 읽어 검증해 두면 그 사고를 빌드 단계에서 잡는다.
 */
@DisplayName("상품 인덱스 설정 파일")
class ProductIndexSettingsTest {

    private static final String PATH = "elasticsearch/product-index-settings.json";

    private JsonNode analysis;

    @BeforeEach
    void setUp() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(PATH)) {
            assertThat(in).as("설정 파일이 클래스패스에 있어야 한다").isNotNull();
            analysis = new ObjectMapper().readTree(in).get("analysis");
        }
    }

    @Test
    @DisplayName("korean 애널라이저가 nori 토크나이저와 커스텀 품사 필터로 조립된다")
    void korean_애널라이저_구성() {
        // given
        final JsonNode korean = analysis.get("analyzer").get("korean");

        // when
        final List<String> filters = toList(korean.get("filter"));

        // then
        assertThat(korean.get("tokenizer").asText()).isEqualTo("korean_tokenizer");
        assertThat(analysis.get("tokenizer").get("korean_tokenizer").get("type").asText())
            .isEqualTo("nori_tokenizer");
        assertThat(filters).containsExactly("korean_pos", "cjk_width", "lowercase", "asciifolding");
    }

    @Test
    @DisplayName("한자를 한글 독음으로 바꾸는 필터를 쓰지 않는다")
    void readingform_미사용() {
        // given
        final List<String> filters = toList(analysis.get("analyzer").get("korean").get("filter"));

        // when & then
        // 한자는 검색 도달 대상이 아니라 이 필터가 할 일이 없다. 게다가 일본 신자체는 절반만 변환돼
        // "한자 중 일부만 한글로 찾히는" 설명 불가능한 상태를 만든다.
        assertThat(filters).doesNotContain("nori_readingform");
    }

    @Test
    @DisplayName("품사 필터가 조사·어미·구두점만 버리고 잎 태그로 나열된다")
    void 품사_필터_stoptags() {
        // given
        final List<String> stoptags = toList(analysis.get("filter").get("korean_pos").get("stoptags"));

        // when & then
        // 부모 태그 J·E는 Lucene POS.Tag enum에 없어 인덱스 생성이 400으로 실패한다. 잎 태그 나열이 필수다.
        assertThat(stoptags).doesNotContain("J", "E");
        assertThat(stoptags).contains("JKS", "JKO", "JC", "JX", "EC", "EF", "ETM");
        // 부사(MAG)·접미사(XSN)를 버리면 「아니 벌써」가 토큰 0개가 되어 도달 불가가 된다
        assertThat(stoptags).doesNotContain("MAG", "MAJ", "MM", "XSN", "XSV", "XSA");
    }

    @Test
    @DisplayName("표기 흔들림을 잡는 필터가 korean·latin 양쪽에 걸린다")
    void 표기_정규화_필터() {
        // given
        final List<String> korean = toList(analysis.get("analyzer").get("korean").get("filter"));
        final List<String> latin = toList(analysis.get("analyzer").get("latin").get("filter"));

        // when & then
        // 사용자는 bjork 라고 치고 저장된 값은 Björk 다. 이 필터가 없으면 둘은 영영 만나지 못한다.
        assertThat(korean).contains("cjk_width", "lowercase", "asciifolding");
        assertThat(latin).contains("cjk_width", "lowercase", "asciifolding");
        // cjk_width 가 lowercase 보다 앞이어야 전각 대문자 Ｂ 가 b 까지 내려간다
        assertThat(korean.indexOf("cjk_width")).isLessThan(korean.indexOf("lowercase"));
        assertThat(latin.indexOf("cjk_width")).isLessThan(latin.indexOf("lowercase"));
    }

    @Test
    @DisplayName("latin 애널라이저는 표준 토크나이저를 쓰고 형태소 분해를 하지 않는다")
    void latin_애널라이저_구성() {
        // given
        final JsonNode latin = analysis.get("analyzer").get("latin");

        // when
        final List<String> filters = toList(latin.get("filter"));

        // then
        assertThat(latin.get("tokenizer").asText()).isEqualTo("standard");
        assertThat(filters).doesNotContain("korean_pos");
    }


    private List<String> toList(final JsonNode array) {
        final List<String> values = new ArrayList<>();
        array.forEach(node -> values.add(node.asText()));
        return values;
    }
}
