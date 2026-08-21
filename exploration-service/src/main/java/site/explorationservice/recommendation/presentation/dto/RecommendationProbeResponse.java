package site.explorationservice.recommendation.presentation.dto;

import site.explorationservice.recommendation.application.dto.RecommendationResult;

/**
 * 프로브(테스트) 전용 추천 응답. 운영 {@link RecommendationResponse}와 달리 genre·label·releaseCountry·pressType까지
 * 내려준다
 */
public record RecommendationProbeResponse(
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

    public static RecommendationProbeResponse from(final RecommendationResult result) {
        return new RecommendationProbeResponse(
            result.productId(),
            result.title(),
            result.artistName(),
            result.genre(),
            result.label(),
            result.releaseYear(),
            result.releaseCountry(),
            result.pressType(),
            result.score()
        );
    }
}
