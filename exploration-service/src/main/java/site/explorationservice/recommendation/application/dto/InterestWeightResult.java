package site.explorationservice.recommendation.application.dto;

import site.explorationservice.productindex.domain.AxisWeights;

/**
 * 위시리스트를 보고 LLM이 산출한 3축 가중치. 가중치 합은 1
 * <p>
 * 세 축은 서로 배타적인 "사용자 타입"이 아니라 비율이다 — 한 사용자 안에서도 섞여 있을 수 있다. 예를 들어 identity 0.5, origin 0.5, edition
 * 0.0이면 "음악적 정체성"과 "시공간적 배경"을 반반으로 보는 타입이다.
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

    public AxisWeights toAxisWeights() {
        return new AxisWeights(identityWeight, originWeight, editionWeight);
    }
}
