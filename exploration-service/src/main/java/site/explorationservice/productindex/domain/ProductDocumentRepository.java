package site.explorationservice.productindex.domain;

import java.util.List;
import java.util.Map;

public interface ProductDocumentRepository {

    void saveAll(List<ProductDocument> documents);

    /**
     * 색인을 쓰는 대상 인덱스에 벡터 필드가 dense_vector 타입으로 잡혀 있는지 확인한다. 인덱스가 아예 없거나, 문서 저장이 먼저 일어나 ES가 타입을
     * 추측해 만든 인덱스라면 false다 — 그런 인덱스는 벡터가 숫자 배열로 굳어 kNN 검색이 통째로 안 되는데도 색인 자체는 에러 없이 성공한다.
     */
    boolean hasVectorMapping();

    /**
     * 색인된 상품의 identity·origin·edition 3벡터를 되읽는다. <b>찾은 것만 담아 돌려준다</b> — 아직 색인되지 않은 상품이 섞여 있어도 실패로
     * 보지 않는다.
     * <p>
     * 목록을 받는 이유는 추천이 위시리스트에 담긴 상품 벡터를 한꺼번에 필요로 하기 때문이다. 한 건씩 읽으면 담은 상품 수만큼 왕복이 생긴다.
     */
    Map<Long, ProductVectors> findVectors(List<Long> productIds);

    /**
     * 주어진 질의 벡터(축마다 하나씩)와 가중치로 가까운 상품을 찾는다. 살아 있는 상품만 대상이다.
     * <p>
     * 내부적으로 축마다 kNN을 따로 돌리고 점수를 가중합해서 병합한다 — 하나로 합친 벡터로 kNN을 한 번만 돌리는 것과 다르다.
     * <p>
     * excludeIds는 <b>질의 단계에서 제외</b>된다. 결과를 받은 뒤에 걸러내면 size개를 채우지 못한다.
     */
    List<ScoredProduct> findSimilar(ProductVectors queryVectors, AxisWeights weights,
        List<Long> excludeIds, int size);
}
