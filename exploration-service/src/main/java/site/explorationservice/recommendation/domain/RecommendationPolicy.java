package site.explorationservice.recommendation.domain;

/**
 * 추천 결과 개수의 기본값과 상한.
 * <p>
 * 상한을 두는 건 kNN이 후보를 size에 비례해 훑기 때문이다 — 요청 하나가 인덱스를 통째로 뒤지게 두지 않는다.
 */
public final class RecommendationPolicy {

    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 50;

    private RecommendationPolicy() {
    }

    /**
     * 0 이하가 오면 기본값으로 되돌린다 — 요청이 잘못됐다고 실패시킬 만큼 중요한 값이 아니다.
     */
    public static int clampSize(final int size) {
        if (size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
