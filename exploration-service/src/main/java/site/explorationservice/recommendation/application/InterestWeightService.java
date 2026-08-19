package site.explorationservice.recommendation.application;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;
import site.explorationservice.ai.chat.application.ChatService;
import site.explorationservice.recommendation.application.dto.InterestWeightResult;
import site.explorationservice.recommendation.application.port.dto.WishlistProduct;
import site.explorationservice.recommendation.exception.RecommendationErrorCode;
import site.explorationservice.recommendation.exception.RecommendationException;

/**
 * 위시리스트를 보고 3축(identity·origin·edition) 가중치를 LLM으로 산출한다.
 * <p>
 * 통계(필드별 distinct 개수)만으로는 부족하다는 게 실측으로 확인됐다 — 예를 들어 "레이블이 다양하다"는 통계는 같아도 전부 메이저 재발매반인지 희귀 인디
 * 오리지널반인지에 따라 뜻이 다른데, 이 구분은 음악 씬 지식이 있어야 가능하다. 근거는 docs/recommendation-avg-test-results.md 5차,
 * docs/search-recommendation-design-notes.md "클러스터링 · 가중치 최종 목표 아키텍처" 참고.
 * <p>
 * 관심사 클러스터링(무관한 관심사를 나누는 것)과는 다른 층위의 문제를 푼다 — 이 서비스는 이미 한 클러스터로 묶인 상품들 <b>안에서</b>의 가중치를 정하는 것이지,
 * 클러스터를 나누는 로직이 아니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterestWeightService {

    private static final BeanOutputConverter<InterestWeightResult> OUTPUT_CONVERTER =
        new BeanOutputConverter<>(InterestWeightResult.class);

    private static final String PROMPT_TEMPLATE = """
        당신은 LP(바이닐 레코드) 수집 취향을 분석하는 어시스턴트입니다.
        사용자의 위시리스트 상품 목록을 보고, 추천 시 아래 세 축에 각각 얼마나 비중을 둬야 할지 판단하세요.
        
        - identity(음악적 정체성): 장르·아티스트가 일관되면 이 축이 중요합니다.
        - origin(시공간적 배경): 발매 연대·국가가 일관되면 이 축이 중요합니다.
        - edition(에디션·수집 가치): 특정 레이블·프레스타입(특히 초판/희귀반)을 일관되게 모으고 있다면 이 축이 중요합니다.
          단순히 레이블이 다양하다고 이 축이 중요한 게 아닙니다 — 메이저 레이블의 흔한 재발매반 위주라면 이 축은 낮게, 희귀
          인디 레이블·초판 위주라면 높게 판단하세요.
        
        세 값의 합이 1이 되도록 비율로 답하고, 왜 그렇게 판단했는지 한 줄 근거를 남기세요.
        
        위시리스트:
        %s
        
        %s
        """;

    private final ChatService chatService;

    public InterestWeightResult analyzeWeights(final List<WishlistProduct> products) {
        final String itemsText = describeItems(products);
        final String prompt = PROMPT_TEMPLATE.formatted(itemsText, OUTPUT_CONVERTER.getFormat());

        try {
            final String content = chatService.call(prompt);
            return OUTPUT_CONVERTER.convert(content);
        } catch (final RuntimeException e) {
            log.warn("관심사 가중치 추론 실패 — 상품 {}건", products.size(), e);
            throw new RecommendationException(
                RecommendationErrorCode.INTEREST_WEIGHT_INFERENCE_FAILED,
                "관심사 가중치 추론에 실패했습니다 — 상품 " + products.size() + "건", e);
        }
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
