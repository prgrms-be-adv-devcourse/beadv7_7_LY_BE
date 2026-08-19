package site.explorationservice.recommendation.application.dto;

/**
 * 위시리스트를 보고 LLM이 산출한 3축 가중치. 세 값의 합이 1이 되도록 프롬프트에서 요청하지만, LLM 출력은 강제되지 않으므로 호출자가 정규화해서 써야 한다.
 * <p>
 * 세 축은 서로 배타적인 "사용자 타입"이 아니라 비율이다 — 한 사용자 안에서도 섞여 있을 수 있다. 근거는
 * docs/search-recommendation-design-notes.md의 "클러스터링 · 가중치 최종 목표 아키텍처" 참고.
 */
public record InterestWeightResult(
    // 장르 + 아티스트 ("음악적 정체성")
    double identityWeight,
    // 발매 연대 + 국가 ("시공간적 배경")
    double originWeight,
    // 레이블 + 프레스타입(초판/재판) ("에디션 · 수집 가치")
    double editionWeight,
    // LLM이 이 비율로 판단한 한 줄 근거 — 결과를 신뢰할지 사람이 눈으로 검증하기 위함
    String rationale
) {

}
