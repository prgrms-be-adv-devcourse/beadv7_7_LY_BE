package site.explorationservice.productindex.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import site.explorationservice.ai.embedding.application.EmbeddingService;
import site.explorationservice.ai.embedding.application.dto.EmbeddingResult;
import site.explorationservice.productindex.application.dto.ProductIndexCommand;
import site.explorationservice.productindex.application.dto.ProductIndexResult;
import site.explorationservice.productindex.domain.ProductDocument;

/**
 * 임베딩 → 색인 → 조회 경로를 실제 ES로 검증한다. 실행 전제: docker/local의 elasticsearch 컨테이너 기동(localhost:9200).
 * <p>
 * <b>EmbeddingService는 목으로 대체한다.</b> 여기서 보려는 건 벡터가 dense_vector 필드에 제대로 색인되고
 * 검색되는가이지 OpenAI 응답의 내용이 아니다. 목으로 두면 <b>과금이 없고</b> 키가 없는 팀원도 돌릴 수 있으며, 실패했을 때 원인이 색인 쪽이라는 게 분명해진다.
 * <p>
 * <b>이 테스트는 실제 인덱스(lp_products)에 쓴다.</b> ProductIndexService가 쓸 인덱스를 ProductDocument에서
 * 가져오므로 매핑 검증 테스트처럼 인덱스를 갈아끼울 수 없다. 인덱스 이름을 주입 가능하게 만들면 격리되지만, 테스트 사정으로 운영 코드에 설정 지점을 만드는 게 되어 택하지
 * 않았다.
 * <p>
 * 대신 세 가지로 격리한다:
 * <ul>
 * <li><b>음수 id를 쓴다</b> — DB 시퀀스가 만들 수 없는 값이라 실제 상품과 겹칠 가능성이 이론적으로도 없다.
 *     ES의 _id는 문자열이라 음수도 유효하다.</li>
 * <li><b>테스트 전후로 지운다</b> — 앞선 실행이 중간에 죽어 남긴 문서가 다음 실행에 영향을 주지 않는다.</li>
 * <li><b>kNN 단언에 id 필터를 건다</b> — kNN은 근사 검색이라 인덱스에 실제 상품이 쌓이면 numCandidates
 *     안에 우리 문서가 안 들어와 엉뚱한 결과가 나올 수 있다. 필터로 후보를 우리 문서로 좁혀 그 영향을 없앤다.</li>
 * </ul>
 */
@Tag("integration")
@SpringBootTest
@DisplayName("상품 색인")
class ProductIndexIntegrationTest {

    /**
     * DB 시퀀스가 만들 수 없는 값이라 실제 상품과 겹치지 않는다.
     */
    private static final Long PRODUCT_ID = -999_999L;
    private static final int DIMENSIONS = 1024;

    @MockitoBean
    private EmbeddingService embeddingService;

    @Autowired
    private ProductIndexService productIndexService;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    private float[] vector;

    @BeforeEach
    void setUp() {
        final IndexOperations indexOperations =
            elasticsearchOperations.indexOps(ProductDocument.class);
        if (!indexOperations.exists()) {
            indexOperations.createWithMapping();
        }

        // 앞선 실행이 중간에 죽어 문서를 남겼을 수 있으므로 시작할 때도 지운다.
        deleteTestDocument();

        vector = randomUnitVector();
        given(embeddingService.embed(anyList(), any(), any()))
            .willReturn(new EmbeddingResult(
                List.of(vector), "text-embedding-3-large", 25, 25));
    }

    @AfterEach
    void tearDown() {
        deleteTestDocument();
    }

    private void deleteTestDocument() {
        elasticsearchOperations.delete(String.valueOf(PRODUCT_ID), ProductDocument.class);
    }

    /**
     * 색인한 벡터가 실제로 kNN 검색에 쓰이는지 확인한다.
     * <p>
     * <b>ES 9는 dense_vector를 _source에서 제외한다.</b> 그래서 {@code get()}으로 문서를 읽으면 다른 필드는
     * 다 오는데 contentVector만 null이다. 벡터가 저장되지 않은 것처럼 보이지만 실제로는 별도 구조에 원본 값 그대로 들어 있고, 검색의 fields
     * 파라미터로 꺼낼 수 있다.
     * <p>
     * 그래서 "저장한 float 배열이 그대로 되읽히는가" 대신 <b>"그 벡터로 검색이 되는가"</b>를 검증한다. 우리가 실제로 필요한 능력이 그쪽이고, 매핑이 잘못돼
     * 벡터가 검색에 안 걸리는 상황도 이 단언이 잡아낸다.
     */
    @Test
    @DisplayName("색인한 벡터가 kNN 검색으로 찾아진다")
    void 벡터_검색() {
        productIndexService.index(command(), ProductEmbeddingTemplate.COMPACT);
        elasticsearchOperations.indexOps(ProductDocument.class).refresh();

        final List<Float> queryVector = new ArrayList<>(DIMENSIONS);
        for (final float value : vector) {
            queryVector.add(value);
        }

        // 필터로 후보를 이 테스트 문서로 좁힌다 — 인덱스에 실제 상품이 쌓여도 근사 검색의 후보 선정에
        // 밀려 결과가 흔들리지 않는다.
        final NativeQuery knnQuery = NativeQuery.builder()
            .withKnnSearches(knn -> knn
                .field("contentVector")
                .queryVector(queryVector)
                .k(1)
                .numCandidates(10)
                .filter(f -> f.term(t -> t.field("productId").value(PRODUCT_ID))))
            .build();

        final SearchHits<ProductDocument> hits =
            elasticsearchOperations.search(knnQuery, ProductDocument.class);

        assertThat(hits.getSearchHits()).isNotEmpty();
        assertThat(hits.getSearchHit(0).getContent().getProductId()).isEqualTo(PRODUCT_ID);
        // 같은 벡터로 찾았으니 유사도가 최댓값에 가까워야 한다 — 엉뚱한 필드를 뒤졌다면 이 값이 낮게 나온다.
        assertThat(hits.getSearchHit(0).getScore()).isGreaterThan(0.99f);
    }

    @Test
    @DisplayName("표시용 필드가 함께 저장된다")
    void 표시용_필드() {
        productIndexService.index(command(), ProductEmbeddingTemplate.COMPACT);

        final ProductDocument found = elasticsearchOperations.get(
            String.valueOf(PRODUCT_ID), ProductDocument.class);

        assertThat(found.getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(found.getTitle()).isEqualTo("Kind of Blue");
        assertThat(found.getArtistName()).isEqualTo("Miles Davis");
        assertThat(found.getActive()).isTrue();
    }

    @Test
    @DisplayName("실제로 임베딩에 넣은 텍스트를 결과로 돌려준다")
    void 임베딩_텍스트_반환() {
        final ProductIndexResult result =
            productIndexService.index(command(), ProductEmbeddingTemplate.COMPACT);

        assertThat(result.embeddedText()).isEqualTo("""
            Miles Davis · Jazz · Columbia
            1950년대 · 미국 · 오리지널 프레스""");
        assertThat(result.dimensions()).isEqualTo(DIMENSIONS);
        assertThat(result.embeddingModel()).isEqualTo("text-embedding-3-large");
    }

    /**
     * 문서 id가 productId라 재색인이 덮어쓰기가 된다. 상품 변경 이벤트를 구독할 때 같은 이벤트를 여러 번 받아도 중복 문서가 생기지 않는다는 뜻이라, 리스너를
     * 붙이기 전에 고정해 둔다.
     */
    @Test
    @DisplayName("같은 상품을 다시 색인해도 문서가 늘지 않고 덮어써진다")
    void 재색인_멱등() {
        productIndexService.index(command(), ProductEmbeddingTemplate.COMPACT);
        productIndexService.index(
            new ProductIndexCommand(PRODUCT_ID, "바뀐 제목", "Miles Davis", "Jazz", "Columbia",
                1959, "미국", "ORIGINAL", true),
            ProductEmbeddingTemplate.COMPACT);

        final ProductDocument found = elasticsearchOperations.get(
            String.valueOf(PRODUCT_ID), ProductDocument.class);

        assertThat(found.getTitle()).isEqualTo("바뀐 제목");
    }

    /**
     * null이면 active 필터에 걸려 추천에서 통째로 빠지므로, 살아 있는 것으로 본다.
     */
    @Test
    @DisplayName("active가 비어 있으면 true로 저장한다")
    void active_기본값() {
        productIndexService.index(
            new ProductIndexCommand(PRODUCT_ID, "Kind of Blue", "Miles Davis", "Jazz", "Columbia",
                1959, "미국", "ORIGINAL", null),
            ProductEmbeddingTemplate.COMPACT);

        final ProductDocument found = elasticsearchOperations.get(
            String.valueOf(PRODUCT_ID), ProductDocument.class);

        assertThat(found.getActive()).isTrue();
    }

    private ProductIndexCommand command() {
        return new ProductIndexCommand(PRODUCT_ID, "Kind of Blue", "Miles Davis", "Jazz",
            "Columbia", 1959, "미국", "ORIGINAL", true);
    }

    /**
     * 실제 임베딩과 같은 성격(단위 길이)의 벡터를 만들어 둔다.
     */
    private float[] randomUnitVector() {
        final Random random = new Random(42);
        final float[] values = new float[DIMENSIONS];
        double sumOfSquares = 0;
        for (int i = 0; i < DIMENSIONS; i++) {
            values[i] = (float) random.nextGaussian();
            sumOfSquares += values[i] * values[i];
        }
        final float norm = (float) Math.sqrt(sumOfSquares);
        for (int i = 0; i < DIMENSIONS; i++) {
            values[i] /= norm;
        }
        return values;
    }
}
