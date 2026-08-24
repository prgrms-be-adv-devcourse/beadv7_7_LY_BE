package site.explorationservice.recommendation.domain;

import java.util.Optional;
import site.explorationservice.productindex.domain.AxisWeights;

/**
 * 위시리스트 기반 3축 가중치(identity/origin/edition) 캐시. 비동기로(위시리스트 변경 시점에) 미리 계산해둔 값을 조회·저장
 */
public interface InterestWeightCacheRepository {

    Optional<AxisWeights> find(Long memberId);

    void save(Long memberId, AxisWeights weights);
}
