package site.productservice.presentation.dto;

import java.util.List;
import site.productservice.application.dto.ProductSnapshotResult;

/**
 * 내부 getProductSnapshot 응답(명세 2-1). 쓰는 곳: 경매(07)의 등록 검증·상품 정보 복사, 주문(06)의 표시.
 * ⚠️ 필드 구성은 경매팀과 협의 진행 중 — 잠정. mergedIntoId는 세미 동안 항상 null.
 * <p>
 * titleAliases·artistAliases는 <b>목록 조회(GET /internal/v1/products)에서만 채워진다.</b> 단건 스냅샷에서는
 * 빈 배열이다 — 그 경로를 쓰는 곳에서 별칭을 소비하지 않아 조회만 늘어나기 때문이다. 필요해지면 그때 채운다.
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
        Long discogsMasterId,
        List<String> titleAliases,
        List<String> artistAliases
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
                result.discogsMasterId(),
                result.titleAliases(),
                result.artistAliases()
        );
    }
}
