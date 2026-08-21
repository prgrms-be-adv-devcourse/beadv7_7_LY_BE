package site.explorationservice.recommendation.presentation;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.explorationservice.productindex.domain.AxisWeights;
import site.explorationservice.recommendation.application.InterestWeightService;
import site.explorationservice.recommendation.application.RecommendationService;
import site.explorationservice.recommendation.domain.RecommendationPolicy;
import site.explorationservice.recommendation.presentation.dto.RecommendationProbeRequest;
import site.explorationservice.recommendation.presentation.dto.RecommendationProbeResponse;

// 테스트용, 추천 로직을 수동 실행. useLlm으로 LLM 가중치와 기본값을 같은 시드로 나란히 비교하거나, weights로 임의 비율을 직접 지정할 수 있다.
@Profile("local")
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/exploration/recommendations")
public class RecommendationProbeController {

    private final RecommendationService recommendationService;
    private final InterestWeightService interestWeightService;

    @PostMapping
    public ApiResponse<List<RecommendationProbeResponse>> recommend(
        @RequestBody final RecommendationProbeRequest request) {
        final AxisWeights weights = request.weights() != null
            ? request.weights()
            : request.useLlmOrDefault()
                ? interestWeightService.analyzeWeights(request.products()).toAxisWeights()
                : RecommendationPolicy.DEFAULT_AXIS_WEIGHTS;

        return ApiResponse.success(
            recommendationService
                .recommendFrom(request.productIds(), request.sizeOrDefault(), weights)
                .stream()
                .map(RecommendationProbeResponse::from)
                .toList());
    }
}
