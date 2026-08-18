package site.explorationservice.recommendation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.explorationservice.productindex.domain.ProductDocument;
import site.explorationservice.productindex.domain.ProductDocumentRepository;
import site.explorationservice.productindex.domain.ScoredProduct;
import site.explorationservice.recommendation.application.dto.RecommendationResult;
import site.explorationservice.recommendation.application.port.WishlistPort;
import site.explorationservice.recommendation.application.port.dto.WishlistProduct;
import site.explorationservice.recommendation.domain.RecommendationPolicy;

/**
 * 추천 로직만 떼어 검증한다. ES도 OpenAI도 띄우지 않으므로 <b>CI에서 그대로 돈다</b>.
 * <p>
 * 여기서 보는 것들은 <b>틀려도 예외가 나지 않는 종류</b>다 — 평균이 잘못 계산되거나 제외 목록이 빠져도 추천은 그럴듯한 목록을 돌려주고, 결과가 이상하다는 신고로만
 * 드러난다. kNN이 실제로 도는지는 통합 테스트가 본다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("추천 로직")
class RecommendationServiceTest {

    @Mock
    private ProductDocumentRepository productDocumentRepository;

    @Mock
    private WishlistPort wishlistPort;

    @InjectMocks
    private RecommendationService recommendationService;

    @Captor
    private ArgumentCaptor<float[]> vectorCaptor;

    @Captor
    private ArgumentCaptor<List<Long>> excludeIdsCaptor;

    @Captor
    private ArgumentCaptor<Integer> sizeCaptor;

    @Test
    @DisplayName("씨앗 벡터의 성분별 평균을 질의 벡터로 쓴다")
    void 평균_계산() {
        givenVectors(Map.of(
            1L, new float[]{1.0f, 0.0f, 0.0f},
            2L, new float[]{0.0f, 1.0f, 0.0f},
            3L, new float[]{0.0f, 0.0f, 1.0f}));
        givenSimilar();

        recommendationService.recommendFrom(List.of(1L, 2L, 3L), 5);

        then(productDocumentRepository).should()
            .findSimilar(vectorCaptor.capture(), anyList(), anyInt());
        // 세 축이 균등하게 섞였으므로 각 성분이 1/3이다. 한 벡터만 반영되거나 합만 내면 여기서 걸린다.
        assertThat(vectorCaptor.getValue())
            .usingComparatorWithPrecision(1e-6f)
            .containsExactly(1f / 3, 1f / 3, 1f / 3);
    }

    @Test
    @DisplayName("정규화하지 않는다 — 길이가 순위에 영향을 주지 않는 코사인 유사도를 쓴다")
    void 정규화_안함() {
        givenVectors(Map.of(1L, new float[]{3.0f, 4.0f}));
        givenSimilar();

        recommendationService.recommendFrom(List.of(1L), 5);

        then(productDocumentRepository).should()
            .findSimilar(vectorCaptor.capture(), anyList(), anyInt());
        // 단위 길이로 맞췄다면 (0.6, 0.8)이 됐을 값이다.
        assertThat(vectorCaptor.getValue()).containsExactly(3.0f, 4.0f);
    }

    @Test
    @DisplayName("색인되지 않은 씨앗이 섞여 있어도 나머지로 평균을 낸다")
    void 일부만_색인됨() {
        // 상품 변경 리스너가 붙기 전까지는 이게 정상 상태다 — 담은 상품 전부가 색인돼 있으리라 가정할 수 없다.
        givenVectors(Map.of(
            1L, new float[]{2.0f, 0.0f},
            3L, new float[]{4.0f, 0.0f}));
        givenSimilar();

        recommendationService.recommendFrom(List.of(1L, 2L, 3L), 5);

        then(productDocumentRepository).should()
            .findSimilar(vectorCaptor.capture(), anyList(), anyInt());
        // 3으로 나누면 2.0이 된다 — 없는 씨앗까지 분모에 넣으면 안 된다.
        assertThat(vectorCaptor.getValue()).containsExactly(3.0f, 0.0f);
    }

    @Test
    @DisplayName("색인 여부와 무관하게 씨앗 전체를 결과에서 제외한다")
    void 제외_목록() {
        givenVectors(Map.of(1L, new float[]{1.0f, 0.0f}));
        givenSimilar();

        recommendationService.recommendFrom(List.of(1L, 2L, 3L), 5);

        then(productDocumentRepository).should()
            .findSimilar(any(), excludeIdsCaptor.capture(), anyInt());
        // 2·3은 아직 색인 전이지만 이미 담은 상품이다. 나중에 색인되고 나서 추천에 튀어나오면 안 된다.
        assertThat(excludeIdsCaptor.getValue()).containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("씨앗이 없으면 ES를 아예 부르지 않는다")
    void 빈_씨앗() {
        assertThat(recommendationService.recommendFrom(List.of(), 5)).isEmpty();

        then(productDocumentRepository).should(never()).findVectors(anyList());
        then(productDocumentRepository).should(never()).findSimilar(any(), anyList(), anyInt());
    }

    @Test
    @DisplayName("씨앗이 하나도 색인돼 있지 않으면 검색하지 않는다")
    void 색인된_씨앗_없음() {
        givenVectors(Map.of());

        assertThat(recommendationService.recommendFrom(List.of(1L, 2L), 5)).isEmpty();

        // 평균 낼 대상이 없다. 빈 벡터로 검색하면 의미 없는 결과가 나온다.
        then(productDocumentRepository).should(never()).findSimilar(any(), anyList(), anyInt());
    }

    @Test
    @DisplayName("size가 상한을 넘으면 상한으로, 0 이하면 기본값으로 조정한다")
    void size_보정() {
        givenVectors(Map.of(1L, new float[]{1.0f}));
        givenSimilar();

        recommendationService.recommendFrom(List.of(1L), RecommendationPolicy.MAX_SIZE + 1);
        recommendationService.recommendFrom(List.of(1L), 0);

        then(productDocumentRepository).should(times(2))
            .findSimilar(any(), anyList(), sizeCaptor.capture());
        assertThat(sizeCaptor.getAllValues())
            .containsExactly(RecommendationPolicy.MAX_SIZE, RecommendationPolicy.DEFAULT_SIZE);
    }

    @Test
    @DisplayName("검색 결과를 점수와 함께 그대로 전달한다")
    void 결과_변환() {
        givenVectors(Map.of(1L, new float[]{1.0f}));
        given(productDocumentRepository.findSimilar(any(), anyList(), anyInt()))
            .willReturn(List.of(scored(100L, "Kind of Blue", "Miles Davis", 0.93f)));

        final List<RecommendationResult> results =
            recommendationService.recommendFrom(List.of(1L), 5);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().productId()).isEqualTo(100L);
        assertThat(results.getFirst().title()).isEqualTo("Kind of Blue");
        assertThat(results.getFirst().artistName()).isEqualTo("Miles Davis");
        assertThat(results.getFirst().score()).isEqualTo(0.93f);
    }

    @Test
    @DisplayName("위시리스트에서 가져온 상품 id를 씨앗으로 넘긴다")
    void 회원_추천_씨앗_변환() {
        given(wishlistPort.findRecentProducts(1L, RecommendationPolicy.WISHLIST_LOOKUP_LIMIT))
            .willReturn(List.of(
                wishlistProduct(10L), wishlistProduct(20L)));
        givenVectors(Map.of(10L, new float[]{1.0f, 0.0f}, 20L, new float[]{0.0f, 1.0f}));
        givenSimilar();

        recommendationService.recommendForMember(1L, 5);

        // 위시리스트가 돌려준 상품 id 그대로가 findVectors·findSimilar 양쪽에 씨앗으로 들어가야 한다.
        then(productDocumentRepository).should().findVectors(List.of(10L, 20L));
        then(productDocumentRepository).should()
            .findSimilar(any(), excludeIdsCaptor.capture(), anyInt());
        assertThat(excludeIdsCaptor.getValue()).containsExactly(10L, 20L);
    }

    @Test
    @DisplayName("위시리스트가 비어 있으면 ES를 아예 부르지 않는다")
    void 회원_추천_빈_위시리스트() {
        given(wishlistPort.findRecentProducts(1L, RecommendationPolicy.WISHLIST_LOOKUP_LIMIT))
            .willReturn(List.of());

        assertThat(recommendationService.recommendForMember(1L, 5)).isEmpty();

        then(productDocumentRepository).should(never()).findVectors(anyList());
    }

    private WishlistProduct wishlistProduct(final Long productId) {
        return new WishlistProduct(productId, "제목", "아티스트", "Jazz", "Columbia", 1959, "미국",
            "ORIGINAL");
    }

    private void givenVectors(final Map<Long, float[]> vectors) {
        // 평균은 순서와 무관해야 하지만, 실패했을 때 재현되도록 순서를 고정해 둔다.
        given(productDocumentRepository.findVectors(anyList()))
            .willReturn(new LinkedHashMap<>(vectors));
    }

    private void givenSimilar() {
        given(productDocumentRepository.findSimilar(any(), anyList(), anyInt()))
            .willReturn(List.of());
    }

    private ScoredProduct scored(final Long productId, final String title, final String artistName,
        final float score) {
        return new ScoredProduct(ProductDocument.builder()
            .productId(productId)
            .title(title)
            .artistName(artistName)
            .active(true)
            .build(), score);
    }
}
