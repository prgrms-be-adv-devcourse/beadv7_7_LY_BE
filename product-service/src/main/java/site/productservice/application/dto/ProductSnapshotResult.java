package site.productservice.application.dto;

import site.productservice.domain.Artist;
import site.productservice.domain.PressType;
import site.productservice.domain.Product;

/**
 * 내부 getProductSnapshot 결과(명세 2-1). 경매가 등록 시점에 상품 정보를 복사해 가는(스냅샷) 원천이다.
 * ⚠️ 필드 구성은 경매팀과 협의 진행 중 — 잠정이며 협의 결과에 따라 조정한다.
 * mergedIntoId: 상품 병합은 파이널 기능이라 세미엔 항상 null (Product에 컬럼 자체가 없음).
 */
public record ProductSnapshotResult(
        Long productId,
        String title,
        String artistName,
        String coverImageUrl,
        String genre,
        PressType pressType,
        int releaseYear,
        boolean active,
        Long mergedIntoId
) {
    public static ProductSnapshotResult of(Product product, Artist artist) {
        return new ProductSnapshotResult(
                product.getId(),
                product.getTitle(),
                artist.getName(),
                product.getCoverImage(),
                product.getGenre(),
                product.getPressType(),
                product.getReleaseYear(),
                product.isActive(),
                null
        );
    }
}
