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
import site.explorationservice.recommendation.domain.InterestWeightCacheRepository;
import site.explorationservice.recommendation.domain.RecommendationPolicy;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final ProductDocumentRepository productDocumentRepository;
    private final WishlistPort wishlistPort;
    private final InterestWeightCacheRepository interestWeightCacheRepository;

    /**
     * 운영 추천 경로. 가중치는 위시리스트 변경 시점에 비동기로 미리 계산해둔 캐시만 쓴다 — 요청 경로에서는 LLM을 아예 부르지 않는다
     * <p>
     * 캐시가 비어 있으면(아직 비동기 파이프라인이 못 따라잡았거나, 이 기능이 생기기 전부터 위시리스트를 안 바꾼 멤버) LLM을 동기로 호출하는 대신 기본값으로 폴백.
     * 실제 가중치는 이미 있는 dirty 추적 파이프라인이 다음 위시리스트 변경 때 채워준다.
     */
    public List<RecommendationResult> recommendForMember(final Long memberId, final int size) {
        final List<WishlistProduct> wishlistProducts =
            wishlistPort.findRecentProducts(memberId, RecommendationPolicy.WISHLIST_LOOKUP_LIMIT);
        if (wishlistProducts.isEmpty()) {
            return List.of();
        }

        final List<Long> productIds =
            wishlistProducts.stream().map(WishlistProduct::productId).toList();
        final AxisWeights weights = interestWeightCacheRepository.find(memberId)
            .orElseGet(() -> {
                log.info("가중치 캐시 미스, 기본값으로 폴백 — memberId={}", memberId);
                return RecommendationPolicy.DEFAULT_AXIS_WEIGHTS;
            });

        return recommendFrom(productIds, size, weights);
    }

    /**
     * 가중치를 명시적으로 받는 경로
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
