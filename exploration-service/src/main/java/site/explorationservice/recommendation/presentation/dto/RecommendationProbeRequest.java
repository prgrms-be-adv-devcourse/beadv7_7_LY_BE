package site.explorationservice.recommendation.presentation.dto;

import java.util.List;
import site.explorationservice.recommendation.application.port.dto.WishlistProduct;

/**
 * 위시리스트 배선 없이 씨앗 상품을 직접 받는다. {@code products}가 productId뿐 아니라 genre·label 등 전체 필드를 담는 건, useLlm이
 * true일 때 {@link site.explorationservice.recommendation.application.InterestWeightService}의 프롬프트 재료로 그대로
 * 쓰기 때문이다 — ES엔 이 필드들이 저장되지 않아서(벡터를 만드는 재료일 뿐) 호출자가 직접 실어 보내야 한다.
 * <p>
 * size·useLlm은 선택값이다. size를 비우면 0으로 취급되고 {@code RecommendationPolicy.clampSize}가 기본값으로 되돌린다.
 * useLlm을 비우거나 false로 두면 {@code RecommendationPolicy.DEFAULT_AXIS_WEIGHTS}(균등)를 쓴다 — LLM 가중치와 기본값을
 * 같은 씨앗으로 나란히 비교하기 위한 토글이다.
 */
public record RecommendationProbeRequest(
    List<WishlistProduct> products,
    Integer size,
    Boolean useLlm
) {

    public List<Long> productIds() {
        return products.stream().map(WishlistProduct::productId).toList();
    }

    public int sizeOrDefault() {
        return size == null ? 0 : size;
    }

    public boolean useLlmOrDefault() {
        return useLlm != null && useLlm;
    }
}
