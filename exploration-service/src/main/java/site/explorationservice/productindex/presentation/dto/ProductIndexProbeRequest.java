package site.explorationservice.productindex.presentation.dto;

import site.explorationservice.productindex.application.ProductEmbeddingTemplate;
import site.explorationservice.productindex.application.dto.ProductIndexCommand;

/**
 * 색인할 상품 정보를 직접 담아 보낸다. product-service를 호출하지 않는 건, 지금 확인하려는 게 임베딩과 색인 경로뿐이고 상품 조회용 내부 API가 아직 없기
 * 때문이다.
 * <p>
 * template은 선택값이다. 비우면 COMPACT를 쓰고, 채우면 그 호출에만 적용된다 — EmbeddingProbeRequest가 model·dimensions를
 * 선택적으로 받는 것과 같은 방식이며, 재기동 없이 두 형식을 비교하기 위한 것이다.
 */
public record ProductIndexProbeRequest(
    Long productId,
    String title,
    String artistName,
    String genre,
    String label,
    Integer releaseYear,
    String releaseCountry,
    String pressType,
    Boolean active,
    ProductEmbeddingTemplate template
) {

    public ProductIndexCommand toCommand() {
        return new ProductIndexCommand(productId, title, artistName, genre, label,
            releaseYear, releaseCountry, pressType, active);
    }

    public ProductEmbeddingTemplate templateOrDefault() {
        return template == null ? ProductEmbeddingTemplate.COMPACT : template;
    }
}
