package site.productservice.application.dto;

import java.util.List;
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
        String label,
        PressType pressType,
        int releaseYear,
        String releaseCountry,
        boolean active,
        Long mergedIntoId,
        String catalogNumber,
        Long discogsMasterId,
        List<String> titleAliases,
        List<String> artistAliases
) {
    /**
     * 별칭을 쓰지 않는 경로용 — 목록 조회 밖에서는 소비하는 곳이 없어 빈 목록으로 둔다.
     * <p>
     * 이름으로 나눈 것은 별칭이 빈다는 사실을 호출부에서 보이게 하려는 것이다. 인자 개수만 다르면
     * 목록 경로에서 실수로 이쪽을 불러도 예외 없이 별칭만 조용히 빈다.
     */
    public static ProductSnapshotResult withoutAliases(Product product, Artist artist) {
        return of(product, artist, List.of(), List.of());
    }

    public static ProductSnapshotResult of(Product product, Artist artist,
            List<String> titleAliases, List<String> artistAliases) {
        return new ProductSnapshotResult(
                product.getId(),
                product.getTitle(),
                artist.getName(),
                product.getCoverImage(),
                product.getGenre(),
                product.getLabel(),
                product.getPressType(),
                product.getReleaseYear(),
                product.getReleaseCountry(),
                product.isActive(),
                null,
                product.getCatalogNumber(),
                product.getDiscogsMasterId(),
                titleAliases,
                artistAliases
        );
    }
}
