package site.coreservice.product.application.dto;

import site.coreservice.product.domain.Artist;
import site.coreservice.product.domain.PressType;
import site.coreservice.product.domain.Product;

/**
 * 내부 getProductSnapshot 결과(명세 2-1) — 경매 등록 검증 + 검색 뷰 스냅샷 소스.
 * ⚠️ 스냅샷 필드 셋은 경매팀과 협의 진행 중(스펙 §4) — 잠정이며 협의 결과로 조정한다.
 * mergedIntoId: 병합은 파이널 기능이라 세미엔 항상 null (Product에 컬럼 없음).
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
