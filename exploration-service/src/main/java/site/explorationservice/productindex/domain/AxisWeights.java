package site.explorationservice.productindex.domain;

/**
 * identity·origin·edition 3벡터를 kNN에서 합칠 때 쓰는 가중치. 세 값의 합이 1일 필요는 없다 — 병합 시점에 합으로 나눠 정규화하므로, 호출자(LLM
 * 출력이든 기본값이든)는 상대 비율만 맞으면 된다. 근거는 docs/search-recommendation-design-notes.md "클러스터링 · 가중치 최종 목표
 * 아키텍처" 참고.
 */
public record AxisWeights(
    double identity,
    double origin,
    double edition
) {

}
