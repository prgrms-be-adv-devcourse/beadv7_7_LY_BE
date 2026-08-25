package site.explorationservice.productindex.application.dto;

import java.util.List;

/**
 * 상품 하나를 색인하는 데 필요한 입력.
 * <p>
 * genre·label·releaseYear·releaseCountry·pressType은 identity·origin·edition 임베딩 텍스트를 만드는 재료이자, 동시에
 * {@code ProductDocument}에 표시용으로 그대로 저장된다(추천 결과를 사람이 아티스트명뿐 아니라 종합적으로 판단하기 위해서 —
 * docs/recommendation-3vector-plan.md 1단계 참고). coverImageUrl은 표시 전용이라 임베딩 텍스트에는 들어가지 않는다.
 * <p>
 * 지금은 수동 트리거(백필)가 이 값을 그대로 채워 보내고, 나중에는 상품 변경 이벤트를 받은 리스너가 채운다.
 * <p>
 * titleAliases·artistAliases는 <b>검색 전용</b>이다 — 「비틀즈」로 The Beatles를 찾는 다른 표기를 이어준다.
 * <b>임베딩 텍스트에는 넣지 않는다.</b> 넣으면 벡터 셋을 전부 다시 만들어야 하고, 추천이 쓰는 신호도 아니다.
 * 상품 서비스 내부 API가 아직 내려주지 않아 당분간 빈 목록으로 들어온다.
 * <p>
 * catalogNumber·discogsMasterId도 <b>임베딩 텍스트에 넣지 않는다.</b> 번호는 음악을 설명하지 않아서 벡터에 섞이면
 * 신호를 흐린다. 번호는 번호 검색이, discogsMasterId는 판본 묶기가 쓴다. 번호를 대조용으로 다듬는 일은
 * {@code ProductIndexService}가 색인할 때 한다 — 검색어를 다듬는 것과 같은 함수를 써야 하기 때문이다.
 */
public record ProductIndexCommand(
    Long productId,
    String title,
    String artistName,
    String coverImageUrl,
    String genre,
    String label,
    Integer releaseYear,
    String releaseCountry,
    String pressType,
    Boolean active,
    String catalogNumber,
    Long discogsMasterId,
    List<String> titleAliases,
    List<String> artistAliases
) {

}
