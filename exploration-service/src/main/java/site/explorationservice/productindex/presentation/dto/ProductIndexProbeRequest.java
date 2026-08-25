package site.explorationservice.productindex.presentation.dto;

import java.util.List;
import site.explorationservice.productindex.application.dto.ProductIndexCommand;

/**
 * 색인할 상품 정보를 직접 담아 보낸다. product-service를 호출하지 않는 건, 지금 확인하려는 게 임베딩과 색인 경로뿐이고 상품 조회용 내부 API가 아직 없기
 * 때문이다.
 */
public record ProductIndexProbeRequest(
    Long productId,
    String title,
    String artistName,
    String coverImageUrl,
    String genre,
    String label,
    Integer releaseYear,
    String releaseCountry,
    String pressType,
    Boolean active
) {

    public ProductIndexCommand toCommand() {
        return new ProductIndexCommand(productId, title, artistName, coverImageUrl, genre, label,
            releaseYear, releaseCountry, pressType, active, List.of(), List.of());
    }
}
