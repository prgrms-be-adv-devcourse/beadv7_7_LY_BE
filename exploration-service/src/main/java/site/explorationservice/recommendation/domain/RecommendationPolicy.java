package site.explorationservice.recommendation.domain;

import site.explorationservice.productindex.domain.AxisWeights;

public final class RecommendationPolicy {

    // 추천 목록 기본값
    public static final int DEFAULT_SIZE = 10;

    // 추천 목록 상한
    public static final int MAX_SIZE = 50;

    // 위시리스트에서 받아올 상품 수
    public static final int WISHLIST_LOOKUP_LIMIT = 50;

    /**
     * identity 6 : origin 4 : edition 0. origin·edition은 임베딩 텍스트 카디널리티가 낮아(연대+국가, 레이블+프레스타입) 독립 kNN
     * 축으로 쓰면 장르·아티스트가 무관해도 완전일치로 노이즈가 낀다는 게 실측으로 확인됐다(docs/recommendation-3vector-plan.md 1단계
     * 참고) — 그룹 상한(설계 v2, {@link site.explorationservice.productindex.infrastructure.ProductDocumentRepositoryImpl})으로
     * 후보 자체의 노이즈는 걸러내지만, 기본 가중치도 안전 마진을 갖도록 origin 혼자만 남긴다. origin 하나의 만점 기여(1.0×0.4=0.4)는
     * identity의 현실적인 매치 점수(0.6×0.85~0.95≈0.51~0.57)를 절대 넘지 못해 — 5:3:2 시절 origin+edition 합산 천장(0.5)이 identity
     * 현실치와 거의 동률이었던 문제(1단계 재분석 참고)가 구조적으로 재발하지 않는다.
     * <p>
     * edition이 0인 건 "안 쓴다"가 아니라 "이 기본값에서는 안 쓴다"다 — {@code InterestWeightService}가 위시리스트에서 진짜 수집가
     * 패턴을 감지하면 그 결과값이 이 기본값을 대체한다.
     */
    public static final AxisWeights DEFAULT_AXIS_WEIGHTS =
        new AxisWeights(0.6, 0.4, 0.0);

    private RecommendationPolicy() {
    }

    public static int clampSize(final int size) {
        if (size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
