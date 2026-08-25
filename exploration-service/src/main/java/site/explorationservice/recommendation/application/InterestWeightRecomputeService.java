package site.explorationservice.recommendation.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.explorationservice.productindex.domain.AxisWeights;
import site.explorationservice.recommendation.application.port.WishlistPort;
import site.explorationservice.recommendation.application.port.dto.WishlistProduct;
import site.explorationservice.recommendation.domain.DirtyMemberTracker;
import site.explorationservice.recommendation.domain.DueMember;
import site.explorationservice.recommendation.domain.InterestWeightCacheRepository;
import site.explorationservice.recommendation.domain.RecommendationPolicy;

/**
 * {@link InterestWeightSweepScheduler}가 디바운스 윈도우를 지난 멤버 단위로 호출. 위시리스트를 조회해 LLM으로 가중치를 재계산하고 캐시에 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterestWeightRecomputeService {

    private final WishlistPort wishlistPort;
    private final InterestWeightService interestWeightService;
    private final InterestWeightCacheRepository interestWeightCacheRepository;
    private final DirtyMemberTracker dirtyMemberTracker;

    public void recompute(final DueMember due) {
        try {
            final List<WishlistProduct> wishlistProducts = wishlistPort
                .findRecentProducts(due.memberId(), RecommendationPolicy.WISHLIST_LOOKUP_LIMIT);

            if (!wishlistProducts.isEmpty()) {
                final AxisWeights weights =
                    interestWeightService.analyzeWeights(wishlistProducts).toAxisWeights();
                interestWeightCacheRepository.save(due.memberId(), weights, due.dirtySince());
            }

            dirtyMemberTracker.complete(due.memberId(), due.dirtySince());
        } catch (final Exception e) {
            log.warn("관심 상품 가중치 계산 실패, 다음 스윕에서 재시도 — memberId={}", due.memberId(), e);
            dirtyMemberTracker.release(due.memberId());
        }
    }
}
