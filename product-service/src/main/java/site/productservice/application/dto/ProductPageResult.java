package site.productservice.application.dto;

import java.util.List;

/**
 * 커서 기반 상품 순회 결과. WishlistItemPage와 같은 모양이다 — 상품 백필처럼 전체를 훑는 쪽이 소비한다.
 */
public record ProductPageResult(List<ProductSnapshotResult> items, Long nextCursor,
                                boolean hasNext) {

}
