package site.explorationservice.recommendation.infrastructure;

import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.explorationservice.productindex.domain.AxisWeights;
import site.explorationservice.recommendation.domain.InterestWeightCacheRepository;

@Repository
@RequiredArgsConstructor
public class InterestWeightCacheRepositoryImpl implements InterestWeightCacheRepository {

    private final MemberInterestWeightsElasticsearchRepository elasticsearchRepository;

    @Override
    public Optional<AxisWeights> find(final Long memberId) {
        return elasticsearchRepository.findById(memberId)
            .map(document -> new AxisWeights(
                document.getIdentityWeight(), document.getOriginWeight(),
                document.getEditionWeight()));
    }

    @Override
    public void save(final Long memberId, final AxisWeights weights, final Instant wishlistChangedAt) {
        elasticsearchRepository.save(MemberInterestWeightsDocument.builder()
            .memberId(memberId)
            .identityWeight(weights.identity())
            .originWeight(weights.origin())
            .editionWeight(weights.edition())
            .computedAt(Instant.now())
            .wishlistChangedAt(wishlistChangedAt)
            .build());
    }
}
