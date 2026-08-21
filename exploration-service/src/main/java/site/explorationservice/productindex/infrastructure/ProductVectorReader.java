package site.explorationservice.productindex.infrastructure;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.FieldAndFormat;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import jakarta.json.JsonArray;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;
import site.explorationservice.productindex.domain.ProductDocument;
import site.explorationservice.productindex.domain.ProductVectors;

/**
 * 색인된 상품의 identity·origin·edition 3벡터를 되읽는다. {@link ProductDocumentRepositoryImpl}이 쓰는 부품이라 애플리케이션
 * 계층에는 보이지 않는다.
 * <p>
 * <b>Spring Data Elasticsearch로는 이걸 할 수 없어서 저수준 클라이언트를 쓴다.</b> ES 9는 dense_vector를
 * _source에서 제외하므로 문서를 조회해도 벡터 필드는 null로 온다. 값을 받으려면 검색의 fields 파라미터로 명시해야 하는데, NativeQuery에는
 * fields를 지정할 통로가 없고 SearchHit도 그 응답을 노출하지 않는다.
 */
@Component
@RequiredArgsConstructor
public class ProductVectorReader {

    private static final String IDENTITY_FIELD = "identityVector";
    private static final String ORIGIN_FIELD = "originVector";
    private static final String EDITION_FIELD = "editionVector";

    private final ElasticsearchClient elasticsearchClient;
    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * 찾은 것만 담아 돌려준다 — 아직 색인되지 않은 상품이 섞여 있어도 실패로 보지 않는다. 3벡터 중 하나라도 없으면(색인 중 실패 등) 그 상품은 통째로 뺀다 —
     * 일부만 채워진 채로 가중 병합에 들어가면 특정 축이 조용히 0 취급돼 결과가 왜곡된다.
     */
    public Map<Long, ProductVectors> findVectors(final List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }

        final SearchResponse<Void> response = search(productIds);

        final Map<Long, ProductVectors> vectors = new LinkedHashMap<>();
        for (final Hit<Void> hit : response.hits().hits()) {
            final JsonData identity = hit.fields().get(IDENTITY_FIELD);
            final JsonData origin = hit.fields().get(ORIGIN_FIELD);
            final JsonData edition = hit.fields().get(EDITION_FIELD);
            if (identity != null && origin != null && edition != null) {
                vectors.put(Long.valueOf(hit.id()), new ProductVectors(
                    toFloatArray(identity), toFloatArray(origin), toFloatArray(edition)));
            }
        }
        return vectors;
    }

    private SearchResponse<Void> search(final List<Long> productIds) {
        final String index = elasticsearchOperations
            .getIndexCoordinatesFor(ProductDocument.class).getIndexName();
        // 문서 _id가 곧 productId라 ids 질의면 충분하다. _source는 어차피 벡터를 담고 있지 않으니 끄고,
        // fields로 3벡터를 한 번에 받는다.
        final List<String> ids = productIds.stream().map(String::valueOf).toList();
        final List<FieldAndFormat> fields = List.of(
            new FieldAndFormat.Builder().field(IDENTITY_FIELD).build(),
            new FieldAndFormat.Builder().field(ORIGIN_FIELD).build(),
            new FieldAndFormat.Builder().field(EDITION_FIELD).build());

        try {
            return elasticsearchClient.search(request -> request
                .index(index)
                .size(ids.size())
                .source(source -> source.fetch(false))
                .fields(fields)
                .query(query -> query.ids(idsQuery -> idsQuery.values(ids))), Void.class);
        } catch (final IOException e) {
            throw new UncheckedIOException("상품 벡터 조회 실패 — productIds: " + productIds, e);
        }
    }

    private float[] toFloatArray(final JsonData field) {
        final JsonArray array = field.toJson().asJsonArray();
        final float[] vector = new float[array.size()];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = (float) array.getJsonNumber(i).doubleValue();
        }
        return vector;
    }
}
