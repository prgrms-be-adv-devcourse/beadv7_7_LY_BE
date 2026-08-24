package site.explorationservice.recommendation.application.dto;

import site.explorationservice.productindex.domain.ProductDocument;
import site.explorationservice.productindex.domain.ScoredProduct;

/**
 * 추천된 상품 하나. 색인 문서에 있는 표시용 필드를 그대로 옮겨 담는다 — 운영 응답(위시리스트와 같은 구성:
 * title·artistName·coverImageUrl·releaseYear)과 진단용 응답(genre·label·releaseCountry·pressType까지 포함, 로컬
 * 프로브 전용)이 여기서 갈라져 나간다. 어느 쪽을 내려줄지는 표현 계층(XxxResponse)에서 정한다.
 */
public record RecommendationResult(
    Long productId,
    String title,
    String artistName,
    String coverImageUrl,
    String genre,
    String label,
    Integer releaseYear,
    String releaseCountry,
    String pressType,
    float score
) {

    public static RecommendationResult from(final ScoredProduct scored) {
        final ProductDocument document = scored.document();
        return new RecommendationResult(
            document.getProductId(),
            document.getTitle(),
            document.getArtistName(),
            document.getCoverImageUrl(),
            document.getGenre(),
            document.getLabel(),
            document.getReleaseYear(),
            document.getReleaseCountry(),
            document.getPressType(),
            scored.score()
        );
    }
}
