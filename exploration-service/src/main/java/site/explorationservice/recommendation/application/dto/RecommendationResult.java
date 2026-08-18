package site.explorationservice.recommendation.application.dto;

import site.explorationservice.productindex.domain.ScoredProduct;

/**
 * 추천된 상품 하나.
 * <p>
 * title·artistName은 <b>결과를 사람이 알아보기 위한 것</b>이다. 색인 문서에 어차피 들어 있어 공짜로 딸려오고, 없으면 프로브 응답이 id 나열이라 눈으로
 * 판정할 수가 없다. 공개 API가 이걸 그대로 내려줄지는 표현 계층에서 정한다.
 */
public record RecommendationResult(
    Long productId,
    String title,
    String artistName,
    float score
) {

    public static RecommendationResult from(final ScoredProduct scored) {
        return new RecommendationResult(
            scored.document().getProductId(),
            scored.document().getTitle(),
            scored.document().getArtistName(),
            scored.score()
        );
    }
}
