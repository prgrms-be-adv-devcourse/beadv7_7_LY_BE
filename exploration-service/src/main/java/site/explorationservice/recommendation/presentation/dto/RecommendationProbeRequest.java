package site.explorationservice.recommendation.presentation.dto;

import java.util.List;

/**
 * 위시리스트 배선 없이 씨앗 상품 id를 직접 받는다. 위시리스트 조회 API가 아직 붙기 전에 <b>평균·kNN 로직만 먼저 확인</b>하려는 것이다.
 * <p>
 * size는 선택값이다. 비우면(null) 0으로 취급되고, {@code RecommendationPolicy.clampSize}가 그걸 기본값으로 되돌린다 — "기본값이
 * 뭔지"를 이 클래스가 알 필요가 없게 한다.
 */
public record RecommendationProbeRequest(
    List<Long> productIds,
    Integer size
) {

    public int sizeOrDefault() {
        return size == null ? 0 : size;
    }
}
