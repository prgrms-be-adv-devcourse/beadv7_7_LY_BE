package site.explorationservice.recommendation.domain;

public final class RecommendationPolicy {

    // 추천 목록 기본값
    public static final int DEFAULT_SIZE = 10;

    // 추천 목록 상한
    public static final int MAX_SIZE = 50;

    // 위시리스트에서 받아올 상품 수
    public static final int WISHLIST_LOOKUP_LIMIT = 50;

    private RecommendationPolicy() {
    }

    public static int clampSize(final int size) {
        if (size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
