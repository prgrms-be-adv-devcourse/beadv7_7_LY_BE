package site.explorationservice.productindex.infrastructure;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
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
     * 축마다 병합 전에 몇 배수의 히트를 가져올지.
     */
    private static final int AXIS_OVERSAMPLE = 5;

    /**
     * 같은 (genre,artist)/(decade,country)/(label,pressType) 그룹에서 후보로 몇 개까지 인정할지.
     */
    private static final int MAX_PER_GROUP = 3;

    /**
     * edition 축은 가중치가 이 값 미만이면 kNN 자체를 돌리지 않는다.
     */
    private static final double EDITION_WEIGHT_THRESHOLD = 0.15;

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
     * <p>
     * identity·origin은 항상 돌리고 그룹 상한을 적용한다. edition은 가중치가 {@link #EDITION_WEIGHT_THRESHOLD} 미만이면 아예
     * 건너뛴다
     * <p>
     * 그룹 상한을 적용한 후보만으로 {@code size}를 못 채우면 상한에 걸려 빠졌던 후보(overflow)로 부족분을 채운다 — 평소엔 다양성을 우선하되, size를
     * 못 채우는 것보다는 낫다.
     */
    @Override
    public List<ScoredProduct> findSimilar(final ProductVectors queryVectors,
        final AxisWeights weights, final List<Long> excludeIds, final int size) {
        final int axisHitLimit = size * AXIS_OVERSAMPLE;

        final GroupCapResult identityResult = capByGroup(
            axisHits(IDENTITY_FIELD, queryVectors.identityVector(), excludeIds, axisHitLimit),
            ProductDocument::getIdentityGroupKey);
        final GroupCapResult originResult = capByGroup(
            axisHits(ORIGIN_FIELD, queryVectors.originVector(), excludeIds, axisHitLimit),
            ProductDocument::getOriginGroupKey);
        final GroupCapResult editionResult = weights.edition() < EDITION_WEIGHT_THRESHOLD
            ? GroupCapResult.EMPTY
            : capByGroup(
                axisHits(EDITION_FIELD, queryVectors.editionVector(), excludeIds, axisHitLimit),
                ProductDocument::getEditionGroupKey);

        final double weightSum = weights.identity() + weights.origin() + weights.edition();

        final List<ScoredProduct> ranked = scoreAndSort(
            identityResult.kept(), originResult.kept(), editionResult.kept(), weights, weightSum);
        if (ranked.size() >= size) {
            return ranked.stream().limit(size).toList();
        }

        final Set<Long> rankedIds = new LinkedHashSet<>();
        ranked.forEach(scored -> rankedIds.add(scored.document().getProductId()));

        final List<ScoredProduct> backfill = scoreAndSort(
            identityResult.overflow(), originResult.overflow(), editionResult.overflow(),
            weights, weightSum)
            .stream()
            .filter(scored -> !rankedIds.contains(scored.document().getProductId()))
            .toList();

        return Stream.concat(ranked.stream(), backfill.stream()).limit(size).toList();
    }

    private List<ScoredProduct> scoreAndSort(
        final Map<Long, SearchHit<ProductDocument>> identityHits,
        final Map<Long, SearchHit<ProductDocument>> originHits,
        final Map<Long, SearchHit<ProductDocument>> editionHits,
        final AxisWeights weights, final double weightSum) {
        final Set<Long> candidateIds = new LinkedHashSet<>();
        candidateIds.addAll(identityHits.keySet());
        candidateIds.addAll(originHits.keySet());
        candidateIds.addAll(editionHits.keySet());

        return candidateIds.stream()
            .map(id -> mergedScore(id, identityHits, originHits, editionHits, weights, weightSum))
            .sorted(Comparator.comparingDouble(ScoredProduct::score).reversed())
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
     * 그룹 상한 결과. {@code kept}는 상한 안에서 살아남은 후보(점수순 정렬에 그대로 쓴다), {@code overflow}는 상한에 걸려 빠진 후보 — 평소엔
     * 버리지만 {@code kept}만으로 요청한 size를 못 채울 때 부족분을 메우는 데 쓴다.
     */
    private record GroupCapResult(
        Map<Long, SearchHit<ProductDocument>> kept,
        Map<Long, SearchHit<ProductDocument>> overflow) {

        static final GroupCapResult EMPTY = new GroupCapResult(Map.of(), Map.of());
    }

    /**
     * 점수 내림차순으로 훑으면서 같은 그룹에서 {@link #MAX_PER_GROUP}개를 넘는 후보는 {@code overflow}로 뺀다.
     */
    private GroupCapResult capByGroup(
        final Map<Long, SearchHit<ProductDocument>> hits,
        final Function<ProductDocument, String> groupKey) {
        final Map<String, Integer> groupCounts = new HashMap<>();
        final Map<Long, SearchHit<ProductDocument>> kept = new LinkedHashMap<>();
        final Map<Long, SearchHit<ProductDocument>> overflow = new LinkedHashMap<>();

        for (final Map.Entry<Long, SearchHit<ProductDocument>> entry : hits.entrySet()) {
            final String key = groupKey.apply(entry.getValue().getContent());
            final int count = groupCounts.getOrDefault(key, 0);
            if (count >= MAX_PER_GROUP) {
                overflow.put(entry.getKey(), entry.getValue());
                continue;
            }
            groupCounts.put(key, count + 1);
            kept.put(entry.getKey(), entry.getValue());
        }
        return new GroupCapResult(kept, overflow);
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
