package site.explorationservice.recommendation.presentation.dto;

import site.explorationservice.recommendation.application.dto.RecommendationResult;

public record RecommendationResponse(
    Long productId,
    String title,
    String artistName,
    float score
) {

    public static RecommendationResponse from(final RecommendationResult result) {
        return new RecommendationResponse(
            result.productId(),
            result.title(),
            result.artistName(),
            result.score()
        );
    }
}
