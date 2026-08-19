package site.explorationservice.recommendation.application;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.explorationservice.productindex.domain.AxisWeights;
import site.explorationservice.productindex.domain.ProductDocumentRepository;
import site.explorationservice.productindex.domain.ProductVectors;
import site.explorationservice.recommendation.application.dto.RecommendationResult;
import site.explorationservice.recommendation.application.port.WishlistPort;
import site.explorationservice.recommendation.application.port.dto.WishlistProduct;
import site.explorationservice.recommendation.domain.RecommendationPolicy;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final ProductDocumentRepository productDocumentRepository;
    private final WishlistPort wishlistPort;

    public List<RecommendationResult> recommendForMember(final Long memberId, final int size) {
        final List<Long> productIds = wishlistPort
            .findRecentProducts(memberId, RecommendationPolicy.WISHLIST_LOOKUP_LIMIT)
            .stream()
            .map(WishlistProduct::productId)
            .toList();

        return recommendFrom(productIds, size);
    }

    /**
     * 가중치를 지정하지 않는 경로 — 지금은 항상 기본값(균등)을 쓴다. LLM 가중치를 운영 경로에 연결하는 건 나중 단계다(프로브에서는 이미 가능 —
     * {@code RecommendationProbeController} 참고).
     */
    public List<RecommendationResult> recommendFrom(final List<Long> productIds, final int size) {
        return recommendFrom(productIds, size, RecommendationPolicy.DEFAULT_AXIS_WEIGHTS);
    }

    /**
     * 가중치를 명시적으로 받는 경로 — LLM이 산출한 가중치든 기본값이든, 병합 로직은 가중치가 어디서 왔는지 몰라도 된다. 근거는
     * docs/search-recommendation-design-notes.md "클러스터링 · 가중치 최종 목표 아키텍처" 참고.
     */
    public List<RecommendationResult> recommendFrom(final List<Long> productIds, final int size,
        final AxisWeights weights) {
        if (productIds.isEmpty()) {
            return List.of();
        }

        final ProductVectors queryVectors = getSearchVectors(productIds);
        if (queryVectors == null) {
            return List.of();
        }

        return productDocumentRepository
            .findSimilar(queryVectors, weights, productIds, RecommendationPolicy.clampSize(size))
            .stream()
            .map(RecommendationResult::from)
            .toList();
    }

    // 현재는 씨앗들의 축별 평균. 향후에는 LLM 클러스터링으로 관심사별로 나눠 각각 평균 낼 수 있다.
    private ProductVectors getSearchVectors(final List<Long> productIds) {
        final Map<Long, ProductVectors> vectors = productDocumentRepository.findVectors(productIds);
        if (vectors.isEmpty()) {
            // 상품이 하나도 인덱싱되지 않았으면 평균 낼 대상이 없다. kNN을 돌릴 이유도 없다.
            log.info("추천 생략 — 인덱싱된 상품이 없습니다. productIds: {}", productIds);
            return null;
        }

        return new ProductVectors(
            average(vectors.values(), ProductVectors::identityVector),
            average(vectors.values(), ProductVectors::originVector),
            average(vectors.values(), ProductVectors::editionVector));
    }

    private float[] average(final Collection<ProductVectors> vectors,
        final Function<ProductVectors, float[]> axis) {
        final float[] averaged = new float[axis.apply(vectors.iterator().next()).length];

        for (final ProductVectors vector : vectors) {
            final float[] values = axis.apply(vector);
            for (int i = 0; i < averaged.length; i++) {
                averaged[i] += values[i];
            }
        }
        for (int i = 0; i < averaged.length; i++) {
            averaged[i] /= vectors.size();
        }
        return averaged;
    }
}
