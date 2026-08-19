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
 * 특히 <b>4개 벡터(combined·identity·origin·edition)가 응답 순서대로 올바른 상품·올바른 필드에 붙는 부분</b>이 이 테스트의 핵심이다.
 * 어긋나도 예외가 나지 않고 엉뚱한 상품에 남의 벡터가 박힌 채 색인이 성공하기 때문에, 실행해서는 알아챌 수 없고 추천 결과가 이상하다는 신고로만 드러난다.
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

    @Test
    @DisplayName("4개 벡터가 응답 순서대로(combined·identity·origin·edition 블록) 각자의 필드에 붙는다")
    void 벡터_정렬() {
        // 블록 순서(combined 3개, identity 3개, origin 3개, edition 3개)로 12개를 준다 —
        // ProductIndexService가 이 순서를 전제로 4등분하기 때문에, 순서가 어긋나면 이 테스트가 잡아낸다.
        givenEmbedding(
            v(0), v(1), v(2), // combined: miles, kim, nirvana
            v(3), v(4), v(5), // identity
            v(6), v(7), v(8), // origin
            v(9), v(10), v(11) // edition
        );

        productIndexService.indexAll(commands(), ProductEmbeddingTemplate.COMPACT);

        then(productDocumentRepository).should().saveAll(documentsCaptor.capture());
        final List<ProductDocument> documents = documentsCaptor.getValue();

        assertThat(documents).extracting(ProductDocument::getProductId)
            .containsExactly(1L, 2L, 3L);

        assertThat(documents.get(0).getContentVector()).containsExactly(v(0));
        assertThat(documents.get(1).getContentVector()).containsExactly(v(1));
        assertThat(documents.get(2).getContentVector()).containsExactly(v(2));

        assertThat(documents.get(0).getIdentityVector()).containsExactly(v(3));
        assertThat(documents.get(1).getIdentityVector()).containsExactly(v(4));
        assertThat(documents.get(2).getIdentityVector()).containsExactly(v(5));

        assertThat(documents.get(0).getOriginVector()).containsExactly(v(6));
        assertThat(documents.get(1).getOriginVector()).containsExactly(v(7));
        assertThat(documents.get(2).getOriginVector()).containsExactly(v(8));

        assertThat(documents.get(0).getEditionVector()).containsExactly(v(9));
        assertThat(documents.get(1).getEditionVector()).containsExactly(v(10));
        assertThat(documents.get(2).getEditionVector()).containsExactly(v(11));
    }

    @Test
    @DisplayName("상품이 여럿이어도 임베딩은 한 번만 호출한다 — 텍스트가 4배로 늘어도 호출 횟수는 그대로")
    void 배치_호출() {
        givenEmbedding(v(0), v(1), v(2), v(3), v(4), v(5), v(6), v(7), v(8), v(9), v(10), v(11));

        productIndexService.indexAll(commands(), ProductEmbeddingTemplate.COMPACT);

        // 상품 수만큼 호출이 늘어나면 백필에서 그대로 비용이 된다.
        then(embeddingService).should(times(1)).embed(anyList(), any(), any());
        then(productDocumentRepository).should(times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("상품별로 자기 텍스트가 임베딩된다 — combined·identity·origin·edition 전부")
    void 텍스트_조립() {
        givenEmbedding(v(0), v(1), v(2), v(3), v(4), v(5), v(6), v(7), v(8), v(9), v(10), v(11));

        final List<ProductIndexResult> results =
            productIndexService.indexAll(commands(), ProductEmbeddingTemplate.COMPACT);

        assertThat(results).extracting(ProductIndexResult::embeddedText).containsExactly(
            "Miles Davis · Jazz · Columbia\n1950년대 · 미국 · 오리지널 프레스",
            "김광석 · 포크 · 킹레코드\n1990년대 · 한국 · 재발매반",
            "Nirvana · 그런지 · DGC\n1990년대 · 미국 · 오리지널 프레스"
        );
        assertThat(results).extracting(ProductIndexResult::identityText).containsExactly(
            "Jazz · Miles Davis", "포크 · 김광석", "그런지 · Nirvana"
        );
        assertThat(results).extracting(ProductIndexResult::originText).containsExactly(
            "1950년대 · 미국", "1990년대 · 한국", "1990년대 · 미국"
        );
        assertThat(results).extracting(ProductIndexResult::editionText).containsExactly(
            "Columbia · ORIGINAL", "킹레코드 · REISSUE", "DGC · ORIGINAL"
        );
    }

    @Test
    @DisplayName("active가 비어 있으면 살아 있는 것으로 본다")
    void active_기본값() {
        givenEmbedding(v(0), v(1), v(2), v(3));

        // null이면 active 필터에 걸려 추천에서 통째로 빠지므로, 색인 대상으로 들어온 이상 기본값이 필요하다.
        productIndexService.index(command(1L, "Miles Davis", null),
            ProductEmbeddingTemplate.COMPACT);

        then(productDocumentRepository).should().saveAll(documentsCaptor.capture());
        assertThat(documentsCaptor.getValue().getFirst().getActive()).isTrue();
    }

    @Test
    @DisplayName("active가 false면 그대로 저장한다")
    void active_유지() {
        givenEmbedding(v(0), v(1), v(2), v(3));

        productIndexService.index(command(1L, "Miles Davis", false),
            ProductEmbeddingTemplate.COMPACT);

        then(productDocumentRepository).should().saveAll(documentsCaptor.capture());
        assertThat(documentsCaptor.getValue().getFirst().getActive()).isFalse();
    }

    @Test
    @DisplayName("단건 색인은 그 상품의 결과만 돌려준다")
    void 단건_색인() {
        givenEmbedding(v(0), v(1), v(2), v(3));

        final ProductIndexResult result =
            productIndexService.index(command(1L, "Miles Davis", true),
                ProductEmbeddingTemplate.COMPACT);

        assertThat(result.productId()).isEqualTo(1L);
        assertThat(result.dimensions()).isEqualTo(1);
        assertThat(result.embeddingModel()).isEqualTo(MODEL);
    }

    private void givenEmbedding(final float[]... vectors) {
        given(embeddingService.embed(anyList(), any(), any()))
            .willReturn(new EmbeddingResult(List.of(vectors), MODEL, 30, 30));
    }

    private float[] v(final int marker) {
        return new float[]{marker};
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
