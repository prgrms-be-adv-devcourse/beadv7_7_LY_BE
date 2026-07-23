package site.coreservice.product.domain;

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
}
