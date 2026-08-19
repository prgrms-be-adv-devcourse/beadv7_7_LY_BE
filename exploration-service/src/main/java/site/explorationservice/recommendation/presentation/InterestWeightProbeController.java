package site.explorationservice.recommendation.presentation;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.explorationservice.recommendation.application.InterestWeightService;
import site.explorationservice.recommendation.application.dto.InterestWeightResult;
import site.explorationservice.recommendation.application.port.dto.WishlistProduct;

/**
 * 위시리스트 배선 없이 상품 목록을 직접 받아 {@link InterestWeightService}만 수동 실행한다. 통계 기반 재정렬 실험(오프라인 스크립트,
 * docs/recommendation-avg-test-results.md 5차)과 같은 씨앗을 LLM 판단과 비교해보기 위한 창구다.
 * <p>
 * {@code @Profile("local")}이 필수다 — 임의의 텍스트로 OpenAI를 호출하는 통로라, 운영에 딸려가면 비용이 그대로 청구된다.
 */
@Profile("local")
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/exploration/recommendations/interest-weights")
public class InterestWeightProbeController {

    private final InterestWeightService interestWeightService;

    @PostMapping
    public ApiResponse<InterestWeightResult> analyze(
        @RequestBody final List<WishlistProduct> products) {
        return ApiResponse.success(interestWeightService.analyzeWeights(products));
    }
}
