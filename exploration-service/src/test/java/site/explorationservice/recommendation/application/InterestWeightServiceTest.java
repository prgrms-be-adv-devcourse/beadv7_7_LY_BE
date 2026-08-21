package site.explorationservice.recommendation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.explorationservice.ai.chat.application.ChatService;
import site.explorationservice.productindex.domain.AxisWeights;
import site.explorationservice.recommendation.application.dto.InterestWeightResult;
import site.explorationservice.recommendation.application.port.dto.WishlistProduct;
import site.explorationservice.recommendation.domain.RecommendationPolicy;
import site.explorationservice.recommendation.exception.RecommendationErrorCode;
import site.explorationservice.recommendation.exception.RecommendationException;

/**
 * LLM이 형식은 지키면서도 뜬금없는 값을 줄 수 있다는 걸 전제로, 그 실패 케이스들이 실제로 걸러지는지 검증한다. 멘토 피드백("음수 값을 주지는 않았는지", "출력 형식이
 * 깨지는 경우는 없었는지")에 대한 실측 근거이기도 하다 — docs/recommendation-portfolio-feedback.md 참고.
 * <p>
 * ChatService는 목으로 대체한다 — 여기서 보려는 건 검증 로직이지 실제 LLM 응답이 아니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("관심사 가중치 검증")
class InterestWeightServiceTest {

    @Mock
    private ChatService chatService;

    @InjectMocks
    private InterestWeightService interestWeightService;

    @Test
    @DisplayName("정상 값이면 그대로 돌려준다")
    void 정상_값() {
        given(chatService.call(anyString(), eq(InterestWeightResult.class)))
            .willReturn(new InterestWeightResult(0.5, 0.3, 0.2, "장르·아티스트가 일관됨"));

        final InterestWeightResult result = interestWeightService.analyzeWeights(products());

        assertThat(result.identityWeight()).isEqualTo(0.5);
        assertThat(result.originWeight()).isEqualTo(0.3);
        assertThat(result.editionWeight()).isEqualTo(0.2);
    }

    @Test
    @DisplayName("ChatService 호출 자체가 실패하면 INTEREST_WEIGHT_INFERENCE_FAILED로 감싼다")
    void 호출_실패() {
        given(chatService.call(anyString(), eq(InterestWeightResult.class)))
            .willThrow(new RuntimeException("OpenAI 401"));

        assertThatThrownBy(() -> interestWeightService.analyzeWeights(products()))
            .isInstanceOf(RecommendationException.class)
            .satisfies(e -> assertThat(((RecommendationException) e).getErrorCode())
                .isEqualTo(RecommendationErrorCode.INTEREST_WEIGHT_INFERENCE_FAILED));
    }

    @Test
    @DisplayName("음수 가중치는 INTEREST_WEIGHT_INVALID로 거부한다")
    void 음수_가중치() {
        given(chatService.call(anyString(), eq(InterestWeightResult.class)))
            .willReturn(new InterestWeightResult(-0.1, 0.6, 0.5, "근거"));

        assertThatThrownBy(() -> interestWeightService.analyzeWeights(products()))
            .isInstanceOf(RecommendationException.class)
            .satisfies(e -> assertThat(((RecommendationException) e).getErrorCode())
                .isEqualTo(RecommendationErrorCode.INTEREST_WEIGHT_INVALID));
    }

    @Test
    @DisplayName("가중치 합이 1에서 너무 벗어나면(범위 밖) INTEREST_WEIGHT_INVALID로 거부한다")
    void 합_이상() {
        given(chatService.call(anyString(), eq(InterestWeightResult.class)))
            .willReturn(new InterestWeightResult(5.0, 3.0, 2.0, "근거"));

        assertThatThrownBy(() -> interestWeightService.analyzeWeights(products()))
            .isInstanceOf(RecommendationException.class)
            .satisfies(e -> assertThat(((RecommendationException) e).getErrorCode())
                .isEqualTo(RecommendationErrorCode.INTEREST_WEIGHT_INVALID));
    }

    @Test
    @DisplayName("합이 1은 아니지만 범위(0.8~1.2) 안이면 거부하지 않고 합이 1이 되도록 재정규화한다")
    void 범위_안이면_재정규화() {
        given(chatService.call(anyString(), eq(InterestWeightResult.class)))
            .willReturn(new InterestWeightResult(0.5, 0.3, 0.3, "근거")); // 합 1.1

        final InterestWeightResult result = interestWeightService.analyzeWeights(products());

        assertThat(result.identityWeight()).isEqualTo(0.5 / 1.1);
        assertThat(result.originWeight()).isEqualTo(0.3 / 1.1);
        assertThat(result.editionWeight()).isEqualTo(0.3 / 1.1);
        assertThat(result.identityWeight() + result.originWeight() + result.editionWeight())
            .isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    @DisplayName("근거가 비어 있으면 INTEREST_WEIGHT_INVALID로 거부한다")
    void 빈_근거() {
        given(chatService.call(anyString(), eq(InterestWeightResult.class)))
            .willReturn(new InterestWeightResult(0.5, 0.3, 0.2, "  "));

        assertThatThrownBy(() -> interestWeightService.analyzeWeights(products()))
            .isInstanceOf(RecommendationException.class)
            .satisfies(e -> assertThat(((RecommendationException) e).getErrorCode())
                .isEqualTo(RecommendationErrorCode.INTEREST_WEIGHT_INVALID));
    }

    @Test
    @DisplayName("analyzeWeightsOrDefault — 성공하면 실제 LLM 결과를 축 가중치로 돌려준다")
    void 폴백_성공시_실제값() {
        given(chatService.call(anyString(), eq(InterestWeightResult.class)))
            .willReturn(new InterestWeightResult(0.5, 0.3, 0.2, "근거"));

        final AxisWeights weights = interestWeightService.analyzeWeightsOrDefault(products());

        assertThat(weights).isEqualTo(new AxisWeights(0.5, 0.3, 0.2));
    }

    @Test
    @DisplayName("analyzeWeightsOrDefault — 추론 실패해도 예외 대신 기본 가중치(균등)를 돌려준다")
    void 폴백_실패시_기본값() {
        given(chatService.call(anyString(), eq(InterestWeightResult.class)))
            .willThrow(new RuntimeException("OpenAI 401"));

        final AxisWeights weights = interestWeightService.analyzeWeightsOrDefault(products());

        assertThat(weights).isEqualTo(RecommendationPolicy.DEFAULT_AXIS_WEIGHTS);
    }

    @Test
    @DisplayName("analyzeWeightsOrDefault — 값이 유효하지 않아도 예외 대신 기본 가중치로 폴백한다")
    void 폴백_무효값도_기본값() {
        given(chatService.call(anyString(), eq(InterestWeightResult.class)))
            .willReturn(new InterestWeightResult(-0.1, 0.6, 0.5, "근거"));

        final AxisWeights weights = interestWeightService.analyzeWeightsOrDefault(products());

        assertThat(weights).isEqualTo(RecommendationPolicy.DEFAULT_AXIS_WEIGHTS);
    }

    private List<WishlistProduct> products() {
        return List.of(new WishlistProduct(1L, "제목", "아티스트", "Jazz", "Columbia", 1959, "미국",
            "ORIGINAL"));
    }
}
