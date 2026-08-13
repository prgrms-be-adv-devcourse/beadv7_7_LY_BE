package site.explorationservice.productindex.infrastructure;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Repository;
import site.explorationservice.productindex.domain.ProductDocument;
import site.explorationservice.productindex.domain.ProductDocumentRepository;

/**
 * Spring Data Elasticsearch 리포지토리 인터페이스를 따로 두지 않는다 — 하는 일이 목록 저장 하나뿐이라
 * {@link ElasticsearchOperations}를 바로 쓰는 편이 짧고, 나중에 kNN 조회를 얹을 때도 결국 이쪽이 필요하다.
 */
@Repository
@RequiredArgsConstructor
public class ProductDocumentRepositoryImpl implements ProductDocumentRepository {

    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * 문서 id가 productId라 같은 상품을 다시 저장하면 덮어쓰기가 된다 — 재색인이 자연히 멱등하다.
     */
    @Override
    public void saveAll(final List<ProductDocument> documents) {
        elasticsearchOperations.save(documents);
    }
}
