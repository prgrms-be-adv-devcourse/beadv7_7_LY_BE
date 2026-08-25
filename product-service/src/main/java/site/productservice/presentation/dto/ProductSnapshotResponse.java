package site.productservice.presentation.dto;

import site.productservice.application.dto.ProductSnapshotResult;

/**
 * 내부 getProductSnapshot 응답(명세 2-1). 쓰는 곳: 경매(07)의 등록 검증·상품 정보 복사, 주문(06)의 표시.
 * ⚠️ 필드 구성은 경매팀과 협의 진행 중 — 잠정. mergedIntoId는 세미 동안 항상 null.
 */
public record ProductSnapshotResponse(
        Long productId,
        String title,
        String artistName,
        String coverImageUrl,
        String genre,
        String label,
        String pressType,
        int releaseYear,
        String releaseCountry,
        boolean active,
        Long mergedIntoId,
        String catalogNumber,
        Long masterId
) {
    public static ProductSnapshotResponse from(ProductSnapshotResult result) {
        return new ProductSnapshotResponse(
                result.productId(),
                result.title(),
                result.artistName(),
                result.coverImageUrl(),
                result.genre(),
                result.label(),
                result.pressType().name(),
                result.releaseYear(),
                result.releaseCountry(),
                result.active(),
                result.mergedIntoId(),
                result.catalogNumber(),
                result.masterId()
        );
    }
}
