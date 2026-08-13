package site.explorationservice.productindex.domain;

import java.util.List;

public interface ProductDocumentRepository {

    void saveAll(List<ProductDocument> documents);
}
