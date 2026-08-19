package site.explorationservice.recommendation.domain;

import site.explorationservice.productindex.domain.AxisWeights;

public final class RecommendationPolicy {

    // 추천 목록 기본값
    public static final int DEFAULT_SIZE = 10;

    // 추천 목록 상한
    public static final int MAX_SIZE = 50;

    // 위시리스트에서 받아올 상품 수
    public static final int WISHLIST_LOOKUP_LIMIT = 50;

    // identity·origin·edition 균등 — knn에서 특정 Vector 축을 우대할 근거가 없을 때 쓰는 기본값
    public static final AxisWeights DEFAULT_AXIS_WEIGHTS =
        new AxisWeights(1.0 / 3, 1.0 / 3, 1.0 / 3);

    private RecommendationPolicy() {
    }

    public static int clampSize(final int size) {
        if (size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
