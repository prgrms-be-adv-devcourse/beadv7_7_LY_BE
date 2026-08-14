package site.explorationservice.productindex.infrastructure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Repository;
import site.explorationservice.productindex.domain.ProductDocument;
import site.explorationservice.productindex.domain.ProductDocumentRepository;
import site.explorationservice.productindex.domain.ScoredProduct;

/**
 * Spring Data Elasticsearch 리포지토리 인터페이스를 따로 두지 않는다 — {@link ElasticsearchOperations}를 바로 쓰는 편이 짧고,
 * kNN 질의는 어차피 그쪽으로만 조립할 수 있다.
 */
@Repository
@RequiredArgsConstructor
public class ProductDocumentRepositoryImpl implements ProductDocumentRepository {

    private static final String VECTOR_FIELD = "contentVector";

    /**
     * kNN이 훑어볼 후보 수. 크게 잡을수록 정확하고 느려진다. 요청 수에 비례시키되, size가 작을 때도 후보가 지나치게 좁아지지 않도록 하한을 둔다.
     */
    private static final int MIN_CANDIDATES = 50;
    private static final int CANDIDATES_PER_RESULT = 10;

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
    public Map<Long, float[]> findVectors(final List<Long> productIds) {
        return productVectorReader.findVectors(productIds);
    }

    @Override
    public List<ScoredProduct> findSimilar(final float[] vector, final List<Long> excludeIds,
        final int size) {
        final NativeQuery query = NativeQuery.builder()
            .withKnnSearches(knn -> knn
                .field(VECTOR_FIELD)
                .queryVector(toQueryVector(vector))
                .k(size)
                .numCandidates(Math.max(size * CANDIDATES_PER_RESULT, MIN_CANDIDATES))
                .filter(f -> f.bool(b -> b
                    .filter(active -> active.term(t -> t.field("active").value(true)))
                    .mustNot(excluded -> excluded.ids(i -> i.values(toStringIds(excludeIds)))))))
            .build();

        return elasticsearchOperations.search(query, ProductDocument.class).getSearchHits().stream()
            .map(hit -> new ScoredProduct(hit.getContent(), hit.getScore()))
            .toList();
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
