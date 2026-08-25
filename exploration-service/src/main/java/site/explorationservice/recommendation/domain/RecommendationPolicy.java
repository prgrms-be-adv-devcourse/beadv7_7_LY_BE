package site.explorationservice.recommendation.domain;

import java.time.Duration;
import lombok.NoArgsConstructor;
import site.explorationservice.productindex.domain.AxisWeights;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class RecommendationPolicy {

    // 추천 목록 기본값
    public static final int DEFAULT_SIZE = 10;

    // 추천 목록 상한
    public static final int MAX_SIZE = 50;

    // 위시리스트에서 받아올 상품 수
    public static final int WISHLIST_LOOKUP_LIMIT = 50;

    /**
     * 벡터 검색 기본 가중치 - (장르 아티스트):(발매 국가, 연대)를 6:4의 비율로 반영한다.
     */
    public static final AxisWeights DEFAULT_AXIS_WEIGHTS =
        new AxisWeights(0.6, 0.4, 0.0);

    /**
     * 위시리스트 변경 이벤트가 온 뒤 이 시간만큼 조용해야 비동기 가중치 재계산을 트리거한다(디바운스) — 연속된 변경을 LLM 호출 한 번으로 묶기 위함.
     */
    public static final Duration DEBOUNCE_WINDOW = Duration.ofSeconds(30);

    public static int clampSize(final int size) {
        if (size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
