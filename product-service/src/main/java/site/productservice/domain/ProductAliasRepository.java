package site.productservice.domain;

import java.util.List;
import java.util.Map;

/**
 * 상품 별칭 저장소. 시드 적재(쓰기)와 존재 확인에만 쓴다 — 검색 조회는
 * ProductSearchRepository 담당.
 */
public interface ProductAliasRepository {

    ProductAlias save(ProductAlias alias);

    /**
     * 같은 상품에 같은 정규화 별칭이 이미 있는지 — 시드를 여러 번 실행해도 중복이 안 쌓이게
     * 하는 확인.
     */
    boolean hasAlias(Long productId, String normalizedName);

    /**
     * 상품 여러 건의 별칭 이름을 한 번에 가져온다. 상품마다 따로 조회하면 목록 한 페이지에 조회가
     * 건수만큼 나간다.
     * <p>
     * 별칭이 하나도 없는 상품은 <b>키 자체가 없다.</b> 부르는 쪽에서 빈 목록으로 받는다.
     */
    Map<Long, List<String>> findNamesByProductIds(List<Long> productIds);
}
