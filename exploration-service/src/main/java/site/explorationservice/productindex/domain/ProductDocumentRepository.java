package site.explorationservice.productindex.domain;

import java.util.List;
import java.util.Map;

public interface ProductDocumentRepository {

    void saveAll(List<ProductDocument> documents);

    /**
     * 색인된 상품의 벡터를 되읽는다. <b>찾은 것만 담아 돌려준다</b> — 아직 색인되지 않은 상품이 섞여 있어도 실패로 보지 않는다.
     * <p>
     * 목록을 받는 이유는 추천이 위시리스트에 담긴 상품 벡터를 한꺼번에 필요로 하기 때문이다. 한 건씩 읽으면 담은 상품 수만큼 왕복이 생긴다.
     */
    Map<Long, float[]> findVectors(List<Long> productIds);

    /**
     * 주어진 벡터와 가까운 상품을 찾는다. 살아 있는 상품만 대상이다.
     * <p>
     * excludeIds는 <b>질의 단계에서 제외</b>된다. 결과를 받은 뒤에 걸러내면 size개를 채우지 못한다.
     */
    List<ScoredProduct> findSimilar(float[] vector, List<Long> excludeIds, int size);
}
