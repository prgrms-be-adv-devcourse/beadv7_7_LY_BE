package site.explorationservice.recommendation.presentation;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.common.web.MemberId;
import site.explorationservice.recommendation.application.RecommendationService;
import site.explorationservice.recommendation.presentation.dto.RecommendationResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    /**
     * size는 선택값이다. 비우면 0으로 넘어가고, RecommendationPolicy.clampSize가 기본값으로 되돌린다.
     */
    @GetMapping
    public ApiResponse<List<RecommendationResponse>> recommend(
        @MemberId final Long memberId,
        @RequestParam(required = false, defaultValue = "0") final int size) {
        return ApiResponse.success(
            recommendationService.recommendForMember(memberId, size).stream()
                .map(RecommendationResponse::from)
                .toList());
    }
}
