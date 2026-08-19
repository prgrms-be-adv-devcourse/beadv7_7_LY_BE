package site.explorationservice.productindex.infrastructure;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Repository;
import site.explorationservice.productindex.domain.AxisWeights;
import site.explorationservice.productindex.domain.ProductDocument;
import site.explorationservice.productindex.domain.ProductDocumentRepository;
import site.explorationservice.productindex.domain.ProductVectors;
import site.explorationservice.productindex.domain.ScoredProduct;

/**
 * Spring Data Elasticsearch 리포지토리 인터페이스를 따로 두지 않는다 — {@link ElasticsearchOperations}를 바로 쓰는 편이 짧고,
 * kNN 질의는 어차피 그쪽으로만 조립할 수 있다.
 */
@Repository
@RequiredArgsConstructor
public class ProductDocumentRepositoryImpl implements ProductDocumentRepository {

    private static final String IDENTITY_FIELD = "identityVector";
    private static final String ORIGIN_FIELD = "originVector";
    private static final String EDITION_FIELD = "editionVector";

    /**
     * kNN이 훑어볼 후보 수(ES 내부 ANN 정확도용). 크게 잡을수록 정확하고 느려진다. 요청 수에 비례시키되, size가 작을 때도 후보가 지나치게 좁아지지 않도록
     * 하한을 둔다.
     */
    private static final int MIN_CANDIDATES = 50;
    private static final int CANDIDATES_PER_RESULT = 10;

    /**
     * 축마다 병합 전에 몇 배수의 히트를 가져올지. 좁으면(예: 최종 size만큼만) 재정렬 여지가 없어진다는 게 실측으로 확인됐다 — 사후 재정렬을 top-20으로만
     * 시도했다가 효과가 제한적이었던 사례(docs/recommendation-avg-test-results.md 6차) 참고.
     */
    private static final int AXIS_OVERSAMPLE = 5;

    private final ElasticsearchOperations elasticsearchOperations;
    private final ProductVectorReader productVectorReader;

    /**
     * 문서 id가 productId라 같은 상품을 다시 저장하면 덮어쓰기가 된다 — 재색인이 자연히 멱등하다.
     */
    @Override
    public void saveAll(final List<ProductDocument> documents) {
        elasticsearchOperations.save(documents);
    }

    @Override
    public Map<Long, ProductVectors> findVectors(final List<Long> productIds) {
        return productVectorReader.findVectors(productIds);
    }

    /**
     * 축마다 kNN을 따로 돌리고, 상품별로 세 점수를 가중합해서 다시 정렬한다. ES 쿼리 하나로 여러 dense_vector 필드를 한 번에 채점하는 기능은 안 쓴다 —
     * 가중치를 요청마다 바꿔야 하는데(LLM 산출 vs 기본값), 색인 시점에 가중치를 고정하지 않고는 ES 쿼리 자체로 이걸 표현할 방법이 마땅치 않다.
     */
    @Override
    public List<ScoredProduct> findSimilar(final ProductVectors queryVectors,
        final AxisWeights weights, final List<Long> excludeIds, final int size) {
        final int axisHitLimit = size * AXIS_OVERSAMPLE;

        final Map<Long, SearchHit<ProductDocument>> identityHits =
            axisHits(IDENTITY_FIELD, queryVectors.identityVector(), excludeIds, axisHitLimit);
        final Map<Long, SearchHit<ProductDocument>> originHits =
            axisHits(ORIGIN_FIELD, queryVectors.originVector(), excludeIds, axisHitLimit);
        final Map<Long, SearchHit<ProductDocument>> editionHits =
            axisHits(EDITION_FIELD, queryVectors.editionVector(), excludeIds, axisHitLimit);

        final double weightSum = weights.identity() + weights.origin() + weights.edition();

        final Set<Long> candidateIds = new LinkedHashSet<>();
        candidateIds.addAll(identityHits.keySet());
        candidateIds.addAll(originHits.keySet());
        candidateIds.addAll(editionHits.keySet());

        return candidateIds.stream()
            .map(id -> mergedScore(id, identityHits, originHits, editionHits, weights, weightSum))
            .sorted(Comparator.comparingDouble(ScoredProduct::score).reversed())
            .limit(size)
            .toList();
    }

    private ScoredProduct mergedScore(final Long id,
        final Map<Long, SearchHit<ProductDocument>> identityHits,
        final Map<Long, SearchHit<ProductDocument>> originHits,
        final Map<Long, SearchHit<ProductDocument>> editionHits,
        final AxisWeights weights, final double weightSum) {
        final double score = (weights.identity() * scoreOf(identityHits, id)
            + weights.origin() * scoreOf(originHits, id)
            + weights.edition() * scoreOf(editionHits, id)) / weightSum;

        final ProductDocument document = anyHit(identityHits, originHits, editionHits,
            id).getContent();
        return new ScoredProduct(document, (float) score);
    }

    /**
     * 후보가 그 축의 히트 목록에 없으면 0으로 본다 — 축마다 상위 {@code axisHitLimit}개만 가져오므로, 못 든 후보는 "그 축에서 유사도가 충분히 높지
     * 않았다"는 뜻이지 조회 실패가 아니다.
     */
    private double scoreOf(final Map<Long, SearchHit<ProductDocument>> hits, final Long id) {
        final SearchHit<ProductDocument> hit = hits.get(id);
        return hit == null ? 0.0 : hit.getScore();
    }

    private SearchHit<ProductDocument> anyHit(
        final Map<Long, SearchHit<ProductDocument>> identityHits,
        final Map<Long, SearchHit<ProductDocument>> originHits,
        final Map<Long, SearchHit<ProductDocument>> editionHits, final Long id) {
        final SearchHit<ProductDocument> hit = identityHits.containsKey(id) ? identityHits.get(id)
            : originHits.containsKey(id) ? originHits.get(id) : editionHits.get(id);
        return hit;
    }

    private Map<Long, SearchHit<ProductDocument>> axisHits(final String field, final float[] vector,
        final List<Long> excludeIds, final int k) {
        final NativeQuery query = NativeQuery.builder()
            .withKnnSearches(knn -> knn
                .field(field)
                .queryVector(toQueryVector(vector))
                .k(k)
                .numCandidates(Math.max(k * CANDIDATES_PER_RESULT, MIN_CANDIDATES))
                .filter(f -> f.bool(b -> b
                    .filter(active -> active.term(t -> t.field("active").value(true)))
                    .mustNot(excluded -> excluded.ids(i -> i.values(toStringIds(excludeIds)))))))
            .withMaxResults(k)
            .build();

        final Map<Long, SearchHit<ProductDocument>> hits = new LinkedHashMap<>();
        for (final SearchHit<ProductDocument> hit
            : elasticsearchOperations.search(query, ProductDocument.class).getSearchHits()) {
            hits.put(hit.getContent().getProductId(), hit);
        }
        return hits;
    }

    /**
     * ES 클라이언트가 Float 목록을 받는다 — float[]를 그대로 넘길 수 없다.
     */
    private List<Float> toQueryVector(final float[] vector) {
        final List<Float> queryVector = new ArrayList<>(vector.length);
        for (final float value : vector) {
            queryVector.add(value);
        }
        return queryVector;
    }

    private List<String> toStringIds(final List<Long> productIds) {
        return productIds.stream().map(String::valueOf).toList();
    }
}
