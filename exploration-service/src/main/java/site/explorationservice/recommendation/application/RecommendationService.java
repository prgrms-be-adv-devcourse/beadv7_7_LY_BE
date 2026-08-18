package site.explorationservice.recommendation.application;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.explorationservice.productindex.domain.ProductDocumentRepository;
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

    public List<RecommendationResult> recommendFrom(final List<Long> productIds,
        final int size) {
        if (productIds.isEmpty()) {
            return List.of();
        }

        float[] searchVector = getSearchVector(productIds);
        if (searchVector == null) {
            return List.of();
        }

        return productDocumentRepository
            .findSimilar(searchVector, productIds,
                RecommendationPolicy.clampSize(size))
            .stream()
            .map(RecommendationResult::from)
            .toList();
    }

    // 현재는 저장된 Vector들의 평균. 향후에는 LLM으로 클러스터링하는 방식으로 바뀔 수 있다.
    private float[] getSearchVector(List<Long> productIds) {
        final Map<Long, float[]> vectors = productDocumentRepository.findVectors(productIds);
        if (vectors.isEmpty()) {
            // 상품이 하나도 인덱싱되지 않았으면 평균 낼 대상이 없다. kNN을 돌릴 이유도 없다.
            log.info("추천 생략 — 인덱싱된 상품이 없습니다. productIds: {}", productIds);
            return null;
        }

        return average(vectors.values());
    }

    private float[] average(final Collection<float[]> vectors) {
        final float[] averaged = new float[vectors.iterator().next().length];

        for (final float[] vector : vectors) {
            for (int i = 0; i < averaged.length; i++) {
                averaged[i] += vector[i];
            }
        }
        for (int i = 0; i < averaged.length; i++) {
            averaged[i] /= vectors.size();
        }
        return averaged;
    }
}
