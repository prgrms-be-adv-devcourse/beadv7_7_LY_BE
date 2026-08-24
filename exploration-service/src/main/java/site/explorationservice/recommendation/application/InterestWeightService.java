package site.explorationservice.recommendation.application;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.explorationservice.ai.chat.application.ChatService;
import site.explorationservice.productindex.domain.AxisWeights;
import site.explorationservice.recommendation.application.dto.InterestWeightResult;
import site.explorationservice.recommendation.application.port.dto.WishlistProduct;
import site.explorationservice.recommendation.domain.RecommendationPolicy;
import site.explorationservice.recommendation.exception.RecommendationErrorCode;
import site.explorationservice.recommendation.exception.RecommendationException;

/**
 * 위시리스트를 보고 3축(identity·origin·edition) 가중치를 LLM으로 산출한다. 통계(필드별 distinct 개수)만으로는 부족하다는 게 실측으로 확인됐다
 * 예를 들어 "레이블이 다양하다"는 통계는 같아도 전부 메이저 재발매반인지 희귀 인디 오리지널반인지에 따라 뜻이 다른데, 이 구분은 음악 씬 지식이 있어야 가능하다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterestWeightService {

    /**
     * 가중치 합이 이 범위를 벗어나면 아예 신뢰하지 않는다. 범위 안이면 형식만 살짝 어긋난 것으로 보고 합이 정확히 1이 되도록 재정규화해서 쓴다 — 프롬프트로 "합이
     * 1이 되도록"이라고 요청해도 LLM 출력은 강제되지 않으므로, 어느 정도 벗어나는 건 정상적인 오차로 본다.
     */
    private static final double MIN_WEIGHT_SUM = 0.8;
    private static final double MAX_WEIGHT_SUM = 1.2;

    private static final String PROMPT_TEMPLATE = """
        당신은 LP(바이닐 레코드) 수집 취향을 분석하는 어시스턴트입니다.
        사용자의 위시리스트 상품 목록을 보고, 추천 시 아래 세 축에 각각 얼마나 비중을 둬야 할지 판단하세요.
        
        - identity(음악적 정체성): 장르·아티스트가 일관되면 이 축이 중요합니다.
        - origin(시공간적 배경): 발매 연대·국가가 일관되면 이 축이 중요합니다.
        - edition(에디션·수집 가치): 프레스타입(ORIGINAL/REISSUE)이 한쪽으로 뚜렷하게 쏠려 있는지, 레이블이 메이저인지 희귀·인디인지를
          함께 보고 판단하세요. 단순히 레이블이 동일하거나 다양하다는 것 자체는 기준이 아닙니다.
          - 레이블이 소수 메이저 위주이거나 REISSUE 비중이 있다면 낮게(예: 0.05~0.1) 판단하세요.
          - 레이블이 다양하더라도 희귀·인디 레이블 위주이고 ORIGINAL로 뚜렷하게 일관되면 높게(예: 0.4~0.5) 판단하세요.
        
        세 값의 합이 1이 되도록 비율로 답하고, 왜 그렇게 판단했는지 한 줄 근거를 남기세요.
        
        위시리스트:
        %s
        """;

    private final ChatService chatService;

    public InterestWeightResult analyzeWeights(final List<WishlistProduct> products) {
        final String prompt = PROMPT_TEMPLATE.formatted(describeItems(products));

        final InterestWeightResult result;
        try {
            result = chatService.call(prompt, InterestWeightResult.class);
        } catch (final RuntimeException e) {
            log.warn("관심사 가중치 추론 실패 — 상품 {}건", products.size(), e);
            throw new RecommendationException(
                RecommendationErrorCode.INTEREST_WEIGHT_INFERENCE_FAILED,
                "관심사 가중치 추론에 실패했습니다 — 상품 " + products.size() + "건", e);
        }

        final InterestWeightResult normalized = normalize(result);
        log.info("관심사 가중치 추론 성공 — identity={}, origin={}, edition={}, rationale={}",
            normalized.identityWeight(), normalized.originWeight(), normalized.editionWeight(),
            normalized.rationale());
        return normalized;
    }

    /**
     * {@link #analyzeWeights}가 실패하면(호출 실패든 값이 유효하지 않든) 기본 가중치(균등)로 폴백.
     */
    public AxisWeights analyzeWeightsOrDefault(final List<WishlistProduct> products) {
        try {
            return analyzeWeights(products).toAxisWeights();
        } catch (final RecommendationException e) {
            log.warn("관심사 가중치 추론 실패, 기본값(균등)으로 폴백합니다 — {}", e.getMessage());
            return RecommendationPolicy.DEFAULT_AXIS_WEIGHTS;
        }
    }

    /**
     * 형식은 맞지만 의미가 이상한 값을 걸러내고(음수 가중치, 합이 크게 벗어난 경우, 빈 근거), 통과한 값은 합이 정확히 1이 되도록 재정규화한다
     */
    private InterestWeightResult normalize(final InterestWeightResult result) {
        if (result.identityWeight() < 0 || result.originWeight() < 0
            || result.editionWeight() < 0) {
            throw invalid("음수 가중치 — " + result);
        }

        final double sum = result.identityWeight() + result.originWeight() + result.editionWeight();
        if (sum < MIN_WEIGHT_SUM || sum > MAX_WEIGHT_SUM) {
            throw invalid("가중치 합이 1에서 너무 벗어남(%.3f) — %s".formatted(sum, result));
        }

        if (result.rationale() == null || result.rationale().isBlank()) {
            throw invalid("근거가 비어 있음 — " + result);
        }

        return new InterestWeightResult(
            result.identityWeight() / sum,
            result.originWeight() / sum,
            result.editionWeight() / sum,
            result.rationale());
    }

    private RecommendationException invalid(final String message) {
        log.warn("관심사 가중치 값이 유효하지 않습니다 — {}", message);
        return new RecommendationException(RecommendationErrorCode.INTEREST_WEIGHT_INVALID,
            message);
    }

    private String describeItems(final List<WishlistProduct> products) {
        return products.stream()
            .map(p -> "- %s · %s · %s · %s년대 · %s · %s".formatted(
                p.artistName(), p.genre(), p.label(), decade(p.releaseYear()),
                p.releaseCountry(), p.pressType()))
            .collect(Collectors.joining("\n"));
    }

    private String decade(final Integer releaseYear) {
        return releaseYear == null ? "미상" : String.valueOf(releaseYear / 10 * 10);
    }
}
