package site.explorationservice.recommendation.application.dto;

import site.explorationservice.productindex.domain.ScoredProduct;

/**
 * 추천된 상품 하나.
 * <p>
 * title·artistName·genre·label·releaseYear·releaseCountry·pressType은 <b>결과를 사람이 종합적으로 알아보기 위한 것</b>이다.
 * 색인 문서에 어차피 들어 있어 공짜로 딸려오고, artistName만으로는 장르·연대·에디션이 실제로 맞는 추천인지 판정할 수 없다는 게 실측으로 확인됐다
 * (docs/recommendation-3vector-plan.md 1단계). 공개 API가 이걸 그대로 내려줄지는 표현 계층에서 정한다.
 */
public record RecommendationResult(
    Long productId,
    String title,
    String artistName,
    String genre,
    String label,
    Integer releaseYear,
    String releaseCountry,
    String pressType,
    float score
) {

    public static RecommendationResult from(final ScoredProduct scored) {
        return new RecommendationResult(
            scored.document().getProductId(),
            scored.document().getTitle(),
            scored.document().getArtistName(),
            scored.document().getGenre(),
            scored.document().getLabel(),
            scored.document().getReleaseYear(),
            scored.document().getReleaseCountry(),
            scored.document().getPressType(),
            scored.score()
        );
    }
}
