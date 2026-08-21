package site.explorationservice.productindex.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

/**
 * 애너테이션으로 선언한 매핑이 실제 ES에서 의도한 형태로 만들어지는지 검증한다. 실행 전제: docker/local의 elasticsearch 컨테이너
 * 기동(localhost:9200) — Nori 플러그인이 설치된 커스텀 이미지여야 한다.
 * <p>
 * 이 검증이 필요한 이유는 <b>매핑을 나중에 고치기 어렵기 때문</b>이다. dense_vector의 dims·similarity와 text 필드의 analyzer는 한 번
 * 색인이 시작되면 인덱스를 새로 만들고 전체 재색인해야 바꿀 수 있다. 애너테이션이 의도대로 번역됐는지는 실제로 만들어 보기 전에는 알 수 없다.
 * <p>
 * 특히 KnnSimilarity와 String similarity는 다른 것이다 — 후자는 text 필드의 BM25 설정이고, dense_vector에 적용되는 건 전자다.
 * 잘못 쓰면 조용히 기본값(l2_norm)으로 만들어진다.
 * <p>
 * <b>ProductDocument가 선언한 실제 인덱스(lp_products)는 건드리지 않는다.</b> 인덱스를 만들었다 지우는
 * 테스트라, 그 이름을 그대로 쓰면 로컬에 쌓아둔 색인 데이터가 테스트를 돌릴 때마다 날아간다. 그래서 매핑과 설정만 ProductDocument에서 생성해 <b>테스트 전용
 * 이름의 인덱스</b>에 적용한다 — 검증하려는 건 인덱스 이름이 아니라 애너테이션이 만들어내는 매핑·설정이므로 이 방식으로 충분하다.
 */
@Tag("integration")
@SpringBootTest
@DisplayName("상품 인덱스 매핑")
class ProductIndexMappingIntegrationTest {

    private static final IndexCoordinates TEST_INDEX =
        IndexCoordinates.of("lp-products-mapping-test");

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    private IndexOperations testIndex;

    @BeforeEach
    void setUp() {
        final IndexOperations entityIndex = elasticsearchOperations.indexOps(ProductDocument.class);

        testIndex = elasticsearchOperations.indexOps(TEST_INDEX);
        if (testIndex.exists()) {
            testIndex.delete();
        }
        testIndex.create(entityIndex.createSettings(), entityIndex.createMapping());
    }

    @AfterEach
    void tearDown() {
        if (testIndex.exists()) {
            testIndex.delete();
        }
    }

    @Test
    @DisplayName("identity·origin·edition 3벡터가 전부 dense_vector 1024차원 cosine으로 만들어진다")
    void 벡터_필드_매핑() {
        for (final String field : new String[]{"identityVector", "originVector", "editionVector"}) {
            final Map<String, Object> vector = fieldMapping(field);

            assertThat(vector.get("type")).as("%s.type", field).isEqualTo("dense_vector");
            assertThat(vector.get("dims")).as("%s.dims", field).isEqualTo(1024);
            assertThat(vector.get("similarity")).as("%s.similarity", field).isEqualTo("cosine");
        }
    }

    @Test
    @DisplayName("텍스트 필드에 한국어 분석기가 걸리고, 정확 일치용 keyword 하위 필드가 함께 만들어진다")
    void 텍스트_필드_매핑() {
        final Map<String, Object> title = fieldMapping("title");
        assertThat(title.get("type")).isEqualTo("text");
        assertThat(title.get("analyzer")).isEqualTo("korean");
        assertThat(title).containsKey("fields");

        assertThat(fieldMapping("artistName").get("analyzer")).isEqualTo("korean");
    }

    /**
     * 지금 필터로 쓰는 건 active 하나뿐이다. 검색용 필드는 검색을 붙일 때 추가한다 — 필드 추가는 매핑 업데이트만으로 되고 벡터를 다시 만들 필요가 없어서 미리
     * 넣을 이유가 없다.
     */
    @Test
    @DisplayName("추천에 필요한 최소 필드만 만들어진다")
    void 최소_필드_매핑() {
        assertThat(fieldMapping("active").get("type")).isEqualTo("boolean");
        // _class는 Spring Data ES가 자동으로 넣는 타입 힌트다. 우리가 선언한 필드가 아니라서 여기 함께 적어둔다
        // — 검색 담당자가 이 인덱스를 볼 때 "이건 뭐지" 할 수 있는 자바 구현 세부사항이기도 하다.
        assertThat(properties())
            .containsOnlyKeys("title", "artistName", "active",
                "identityVector", "originVector", "editionVector", "_class");
    }

    /**
     * Nori는 도커 이미지에 번들되어 있지 않아 커스텀 이미지로 설치했다. 플러그인이 빠진 이미지로 돌리면 인덱스 생성 자체가 실패하므로, 여기까지 왔다는 건 설정 파일이
     * 실제로 먹었다는 뜻이다.
     */
    @Test
    @DisplayName("한국어 분석기 설정이 인덱스에 반영된다")
    void 분석기_설정() {
        final Map<String, Object> settings = testIndex.getSettings(true);

        assertThat(settings.get("index.analysis.analyzer.korean.tokenizer"))
            .isEqualTo("korean_tokenizer");
        assertThat(settings.get("index.analysis.tokenizer.korean_tokenizer.type"))
            .isEqualTo("nori_tokenizer");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> properties() {
        return (Map<String, Object>) testIndex.getMapping().get("properties");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fieldMapping(final String fieldName) {
        return (Map<String, Object>) properties().get(fieldName);
    }
}
