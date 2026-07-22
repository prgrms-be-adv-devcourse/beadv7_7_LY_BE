package site.coreservice.product.presentation;

import site.coreservice.product.application.ProductSnapshotResult;

/**
 * 내부 getProduct 응답(명세 2-1). 소비자: 경매(07) 등록 검증+스냅샷, 주문(06) 표시.
 * ⚠️ 스냅샷 필드 셋은 경매팀과 협의 진행 중 — 잠정. mergedIntoId는 세미 항상 null.
 */
public record ProductSnapshotResponse(
        Long productId,
        String title,
        String artistName,
        String coverImageUrl,
        String genre,
        String pressType,
        int releaseYear,
        boolean active,
        Long mergedIntoId
) {
    public static ProductSnapshotResponse from(ProductSnapshotResult result) {
        return new ProductSnapshotResponse(
                result.productId(),
                result.title(),
                result.artistName(),
                result.coverImageUrl(),
                result.genre(),
                result.pressType().name(),
                result.releaseYear(),
                result.active(),
                result.mergedIntoId()
        );
    }
}
