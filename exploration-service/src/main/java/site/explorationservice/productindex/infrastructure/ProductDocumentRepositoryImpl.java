package site.explorationservice.productindex.infrastructure;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
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
     * 같은 (genre,artist)/(decade,country)/(label,pressType) 그룹에서 최종 결과로 몇 개까지 인정할지.
     */
    private static final int MAX_PER_GROUP = 3;

    /**
     * edition 축은 가중치가 이 값 미만이면 kNN 자체를 돌리지 않는다.
     */
    private static final double EDITION_WEIGHT_THRESHOLD = 0.15;

    private final ElasticsearchOperations elasticsearchOperations;
    private final ProductVectorReader productVectorReader;

    /**
     * 평소에는 별칭에 쓴다. 재색인할 때만 아직 별칭이 가리키지 않는 새 인덱스를 지정해, 검색이 옛 인덱스로 서비스되는 동안 새 인덱스를 채운다.
     */
    private final String writeTarget;

    public ProductDocumentRepositoryImpl(
        final ElasticsearchOperations elasticsearchOperations,
        final ProductVectorReader productVectorReader,
        @Value("${exploration.product-index.write-target:lp_products}") final String writeTarget) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.productVectorReader = productVectorReader;
        this.writeTarget = writeTarget;
    }

    /**
     * 문서 id가 productId라 같은 상품을 다시 저장하면 덮어쓰기가 된다 — 재색인이 자연히 멱등하다.
     */
    @Override
    public void saveAll(final List<ProductDocument> documents) {
        elasticsearchOperations.save(documents, IndexCoordinates.of(writeTarget));
    }

    @Override
    public Map<Long, ProductVectors> findVectors(final List<Long> productIds) {
        return productVectorReader.findVectors(productIds);
    }

    /**
     * 축마다 kNN으로 후보를 소싱하고, 후보 전체를 저장된 벡터로 정확하게 채점한 뒤, 점수순으로 그룹 상한을 적용한다.
     * <p>
     * <b>ES kNN 점수는 소싱에만 쓰고 채점에는 안 쓴다.</b>  소싱(후보를 어디까지 찾아볼지)만 ES kNN에 맡기고, 채점(그 후보들의 정확한 점수)은
     * 저장된 벡터로 직접 코사인을 계산한다.
     */
    @Override
    public List<ScoredProduct> findSimilar(final ProductVectors queryVectors,
        final AxisWeights weights, final List<Long> excludeIds, final int size) {
        final int axisHitLimit = size * AXIS_OVERSAMPLE;

        final Map<Long, ProductDocument> identityHits =
            axisHits(IDENTITY_FIELD, queryVectors.identityVector(), excludeIds, axisHitLimit);
        final Map<Long, ProductDocument> originHits =
            axisHits(ORIGIN_FIELD, queryVectors.originVector(), excludeIds, axisHitLimit);
        final Map<Long, ProductDocument> editionHits = weights.edition() < EDITION_WEIGHT_THRESHOLD
            ? Map.of()
            : axisHits(EDITION_FIELD, queryVectors.editionVector(), excludeIds, axisHitLimit);

        final Map<Long, ProductDocument> candidates = new LinkedHashMap<>();
        candidates.putAll(identityHits);
        candidates.putAll(originHits);
        candidates.putAll(editionHits);

        final Map<Long, ProductVectors> candidateVectors =
            productVectorReader.findVectors(List.copyOf(candidates.keySet()));

        final double weightSum = weights.identity() + weights.origin() + weights.edition();

        final List<ScoredProduct> sorted = candidates.entrySet().stream()
            .map(entry -> mergedScore(entry.getValue(), weights, weightSum, queryVectors,
                candidateVectors.get(entry.getKey())))
            .sorted(Comparator.comparingDouble(ScoredProduct::score).reversed())
            .toList();

        return applyGroupCap(sorted, size);
    }

    private ScoredProduct mergedScore(final ProductDocument document, final AxisWeights weights,
        final double weightSum, final ProductVectors queryVectors,
        final ProductVectors candidateVectors) {
        final double score = (weights.identity() * scoreOf(
            queryVectors.identityVector(), candidateVectors, ProductVectors::identityVector)
            + weights.origin() * scoreOf(
            queryVectors.originVector(), candidateVectors, ProductVectors::originVector)
            + weights.edition() * scoreOf(
            queryVectors.editionVector(), candidateVectors, ProductVectors::editionVector))
            / weightSum;

        return new ScoredProduct(document, (float) score);
    }

    /**
     * 후보의 저장된 벡터로 직접 코사인을 계산해 ES의 kNN 점수와 같은 스케일([0,1], (1+cos)/2)로 맞춘다. {@code candidateVectors}가
     * 없는(벡터 조회 자체가 실패한, 사실상 없는) 경우에만 0으로 본다.
     */
    private double scoreOf(final float[] queryVector, final ProductVectors candidateVectors,
        final Function<ProductVectors, float[]> axis) {
        if (candidateVectors == null) {
            return 0.0;
        }
        return (1 + cosineSimilarity(queryVector, axis.apply(candidateVectors))) / 2;
    }

    private double cosineSimilarity(final float[] a, final float[] b) {
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 점수순으로 정렬된 후보를 위에서부터 훑으면서, identity/origin/edition 그룹 중 하나라도 이미 {@link #MAX_PER_GROUP}개가 찼으면
     * 건너뛴다. 통과한 후보만으로 {@code size}를 못 채우면, 건너뛴 후보(원래 점수 순서 그대로)로 부족분을 채운다 — 평소엔 다양성을 우선하되, size를 못
     * 채우는 것보다는 낫다
     */
    private List<ScoredProduct> applyGroupCap(final List<ScoredProduct> sorted, final int size) {
        final Map<String, Integer> identityCounts = new HashMap<>();
        final Map<String, Integer> originCounts = new HashMap<>();
        final Map<String, Integer> editionCounts = new HashMap<>();
        final List<ScoredProduct> accepted = new ArrayList<>();
        final List<ScoredProduct> skipped = new ArrayList<>();

        for (final ScoredProduct scored : sorted) {
            final ProductDocument document = scored.document();
            final String identityKey = document.getIdentityGroupKey();
            final String originKey = document.getOriginGroupKey();
            final String editionKey = document.getEditionGroupKey();

            final boolean overCap = identityCounts.getOrDefault(identityKey, 0) >= MAX_PER_GROUP
                || originCounts.getOrDefault(originKey, 0) >= MAX_PER_GROUP
                || editionCounts.getOrDefault(editionKey, 0) >= MAX_PER_GROUP;

            if (overCap) {
                skipped.add(scored);
                continue;
            }

            identityCounts.merge(identityKey, 1, Integer::sum);
            originCounts.merge(originKey, 1, Integer::sum);
            editionCounts.merge(editionKey, 1, Integer::sum);
            accepted.add(scored);
        }

        if (accepted.size() >= size) {
            return accepted.subList(0, size);
        }
        return Stream.concat(accepted.stream(), skipped.stream()).limit(size).toList();
    }

    private Map<Long, ProductDocument> axisHits(final String field, final float[] vector,
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

        final Map<Long, ProductDocument> hits = new LinkedHashMap<>();
        for (final SearchHit<ProductDocument> hit
            : elasticsearchOperations.search(query, ProductDocument.class).getSearchHits()) {
            hits.put(hit.getContent().getProductId(), hit.getContent());
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
