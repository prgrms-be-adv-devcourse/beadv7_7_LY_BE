package site.explorationservice.productindex.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.explorationservice.ai.embedding.application.EmbeddingService;
import site.explorationservice.ai.embedding.application.dto.EmbeddingResult;
import site.explorationservice.productindex.application.dto.ProductIndexCommand;
import site.explorationservice.productindex.application.dto.ProductIndexResult;
import site.explorationservice.productindex.domain.ProductDocument;
import site.explorationservice.productindex.domain.ProductDocumentRepository;

/**
 * 색인 로직만 떼어 검증한다. ES도 OpenAI도 띄우지 않으므로 <b>CI에서 그대로 돈다</b> — 여기서 보는 것들은 통합 테스트에 두면 `-PexcludeTags`로
 * 빠져 아무도 안 보게 된다.
 * <p>
 * 특히 <b>벡터를 요청 순서대로 상품에 붙이는 부분</b>이 이 테스트의 핵심이다. 어긋나도 예외가 나지 않고 엉뚱한 상품에 남의 벡터가 박힌 채 색인이 성공하기 때문에,
 * 실행해서는 알아챌 수 없고 추천 결과가 이상하다는 신고로만 드러난다.
 * <p>
 * 매핑이 제대로 됐는지, kNN이 실제로 도는지는 여기서 볼 수 없다 — 그건 ProductIndexIntegrationTest가 실제 ES로 확인한다. 이 테스트가 로직을,
 * 그쪽이 배선을 맡는다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("상품 색인 로직")
class ProductIndexServiceTest {

    private static final String MODEL = "text-embedding-3-large";

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private ProductDocumentRepository productDocumentRepository;

    @InjectMocks
    private ProductIndexService productIndexService;

    @Captor
    private ArgumentCaptor<List<ProductDocument>> documentsCaptor;

    private float[] milesVector;
    private float[] kimVector;
    private float[] nirvanaVector;

    @BeforeEach
    void setUp() {
        // 값 자체는 의미 없고, 서로 구별되기만 하면 된다 — 어느 벡터가 어느 상품에 붙었는지 추적하기 위한 표식이다.
        milesVector = new float[]{1.0f, 0.0f, 0.0f};
        kimVector = new float[]{0.0f, 1.0f, 0.0f};
        nirvanaVector = new float[]{0.0f, 0.0f, 1.0f};
    }

    @Test
    @DisplayName("임베딩 응답 순서대로 상품에 벡터가 붙는다")
    void 벡터_정렬() {
        givenEmbedding(milesVector, kimVector, nirvanaVector);

        productIndexService.indexAll(commands(), ProductEmbeddingTemplate.COMPACT);

        then(productDocumentRepository).should().saveAll(documentsCaptor.capture());
        final List<ProductDocument> documents = documentsCaptor.getValue();

        assertThat(documents).extracting(ProductDocument::getProductId)
            .containsExactly(1L, 2L, 3L);
        assertThat(documents.get(0).getContentVector()).containsExactly(milesVector);
        assertThat(documents.get(1).getContentVector()).containsExactly(kimVector);
        assertThat(documents.get(2).getContentVector()).containsExactly(nirvanaVector);
    }

    @Test
    @DisplayName("상품이 여럿이어도 임베딩은 한 번만 호출한다")
    void 배치_호출() {
        givenEmbedding(milesVector, kimVector, nirvanaVector);

        productIndexService.indexAll(commands(), ProductEmbeddingTemplate.COMPACT);

        // 상품 수만큼 호출이 늘어나면 백필에서 그대로 비용이 된다.
        then(embeddingService).should(times(1)).embed(anyList(), any(), any());
        then(productDocumentRepository).should(times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("상품별로 자기 텍스트가 임베딩된다")
    void 텍스트_조립() {
        givenEmbedding(milesVector, kimVector, nirvanaVector);

        final List<ProductIndexResult> results =
            productIndexService.indexAll(commands(), ProductEmbeddingTemplate.COMPACT);

        assertThat(results).extracting(ProductIndexResult::embeddedText).containsExactly(
            "Miles Davis · Jazz · Columbia\n1950년대 · 미국 · 오리지널 프레스",
            "김광석 · 포크 · 킹레코드\n1990년대 · 한국 · 재발매반",
            "Nirvana · 그런지 · DGC\n1990년대 · 미국 · 오리지널 프레스"
        );
    }

    @Test
    @DisplayName("active가 비어 있으면 살아 있는 것으로 본다")
    void active_기본값() {
        givenEmbedding(milesVector);

        // null이면 active 필터에 걸려 추천에서 통째로 빠지므로, 색인 대상으로 들어온 이상 기본값이 필요하다.
        productIndexService.index(command(1L, "Miles Davis", null),
            ProductEmbeddingTemplate.COMPACT);

        then(productDocumentRepository).should().saveAll(documentsCaptor.capture());
        assertThat(documentsCaptor.getValue().getFirst().getActive()).isTrue();
    }

    @Test
    @DisplayName("active가 false면 그대로 저장한다")
    void active_유지() {
        givenEmbedding(milesVector);

        productIndexService.index(command(1L, "Miles Davis", false),
            ProductEmbeddingTemplate.COMPACT);

        then(productDocumentRepository).should().saveAll(documentsCaptor.capture());
        assertThat(documentsCaptor.getValue().getFirst().getActive()).isFalse();
    }

    @Test
    @DisplayName("단건 색인은 그 상품의 결과만 돌려준다")
    void 단건_색인() {
        givenEmbedding(milesVector);

        final ProductIndexResult result =
            productIndexService.index(command(1L, "Miles Davis", true),
                ProductEmbeddingTemplate.COMPACT);

        assertThat(result.productId()).isEqualTo(1L);
        assertThat(result.dimensions()).isEqualTo(milesVector.length);
        assertThat(result.embeddingModel()).isEqualTo(MODEL);
    }

    private void givenEmbedding(final float[]... vectors) {
        given(embeddingService.embed(anyList(), any(), any()))
            .willReturn(new EmbeddingResult(List.of(vectors), MODEL, 30, 30));
    }

    private List<ProductIndexCommand> commands() {
        return List.of(
            new ProductIndexCommand(1L, "Kind of Blue", "Miles Davis", "Jazz", "Columbia",
                1959, "미국", "ORIGINAL", true),
            new ProductIndexCommand(2L, "다시 부르기 2", "김광석", "포크", "킹레코드",
                1995, "한국", "REISSUE", true),
            new ProductIndexCommand(3L, "Nevermind", "Nirvana", "그런지", "DGC",
                1991, "미국", "ORIGINAL", true)
        );
    }

    private ProductIndexCommand command(final Long productId, final String artistName,
        final Boolean active) {
        return new ProductIndexCommand(productId, "제목", artistName, "Jazz", "Columbia",
            1959, "미국", "ORIGINAL", active);
    }
}
